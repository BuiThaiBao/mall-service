package vn.vti.dtn2504.mallservice.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.vti.dtn2504.mallservice.client.payload.request.SendNotificationRequest;
import vn.vti.dtn2504.mallservice.dto.request.OrderRequest;
import vn.vti.dtn2504.mallservice.dto.response.OrderItemResponse;
import vn.vti.dtn2504.mallservice.dto.response.OrderResponse;
import vn.vti.dtn2504.mallservice.dto.response.ProductResponse;
import vn.vti.dtn2504.mallservice.entity.Order;
import vn.vti.dtn2504.mallservice.entity.OrderItem;
import vn.vti.dtn2504.mallservice.entity.Product;
import vn.vti.dtn2504.mallservice.enums.OrderStatus;
import vn.vti.dtn2504.mallservice.repository.OrderRepository;
import vn.vti.dtn2504.mallservice.repository.ProductRepository;
import vn.vti.dtn2504.mallservice.service.OrderService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.notification.routing-key}")
    private String routingKey;
    @Value("${queue.notification.exchange}")
    private String exchangeName;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, String fullName, String email, String address, String phoneNumber, OrderRequest request) {
        log.info("Starting to create order for userId: {}", userId);

        // 1. Validate & Get Products (Tối ưu: Lấy 1 lần, trừ kho 1 lần)
        Map<Long, Product> productMap = validateAndGetProducts(request);

        // 2. Tính tổng tiền
        BigDecimal total = calculateTotal(productMap, request);

        // 3. Tạo Order Entity
        Order order = createOrderEntity(userId, address, request, total);

        // 4. Tạo Order Items & Trừ kho
        List<OrderItemResponse> orderItemResponseList = processOrderItems(order, request, productMap);

        // 5. Lưu Order (Nhớ config CascadeType.ALL trong Entity Order để nó lưu luôn items)
        Order savedOrder = orderRepository.save(order);

        // Lưu lại các Product đã bị trừ kho (Lưu 1 lần batch save)
        productRepository.saveAll(productMap.values());

        // 6. Gửi RabbitMQ (Dùng DTO List nên sẽ không bị lỗi)
        sendToRabbitMQ(savedOrder, fullName, email, phoneNumber, orderItemResponseList);

        return OrderResponse.builder()
                .fullName(fullName)
                .email(email)
                .orderId(savedOrder.getId()) // Lấy ID từ savedOrder cho chắc
                .phoneNumber(phoneNumber)
                .totalPrice(total)
                .orderStatus(String.valueOf(savedOrder.getStatus()))
                .address(savedOrder.getAddress())
                .orderItems(orderItemResponseList)
                .build();
    }

    // Tách hàm gửi RabbitMQ cho gọn code chính
    private void sendToRabbitMQ(Order order, String fullName, String email, String phoneNumber, List<OrderItemResponse> items) {
        try {
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("fullName", fullName);
            orderInfo.put("email", email);
            orderInfo.put("userId", order.getUserId());
            orderInfo.put("phoneNumber", phoneNumber);
            // Convert UUID/Long thành String để bên kia nhận cho dễ
            orderInfo.put("orderId", String.valueOf(order.getId()));
            orderInfo.put("totalPrice", order.getTotalPrice());
            orderInfo.put("orderStatus", String.valueOf(order.getStatus()));
            orderInfo.put("address", order.getAddress());

            // Đây là chìa khóa: Gửi List DTO chứ không gửi List Entity
            orderInfo.put("orderItems", items);

            SendNotificationRequest notificationRequest = new SendNotificationRequest();
            notificationRequest.setMessage("Bạn đã đặt hàng thành công");
            notificationRequest.setTitle("Thông tin đặt hàng");
            notificationRequest.setSendTo(email);
            notificationRequest.setTemplateCode("NEW_ORDER_EVENT");
            notificationRequest.setParam(orderInfo);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, notificationRequest);
            log.info("✅ Notification event sent to RabbitMQ");
        } catch (Exception e) {
            log.error("⚠️ Failed to send notification (Order created but Log failed): {}", e.getMessage());
            // Không throw exception ở đây để tránh Rollback đơn hàng chỉ vì lỗi gửi mail
        }
    }

    private Map<Long, Product> validateAndGetProducts(OrderRequest request) {
        Map<Long, Product> productMap = new HashMap<>();

        // Lấy danh sách ID sản phẩm từ request
        List<Long> productIds = request.getItems().stream()
                .map(item -> item.getProductId())
                .toList();

        // Tối ưu: Gọi DB 1 lần lấy tất cả sản phẩm
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new RuntimeException("Một số sản phẩm không tồn tại hoặc đã bị xóa");
        }

        // Map vào HashMap để dễ truy xuất và check tồn kho
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }

        // Check tồn kho
        for (var item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Không đủ hàng cho sản phẩm: " + product.getName());
            }
        }
        return productMap;
    }

    private BigDecimal calculateTotal(Map<Long, Product> productMap, OrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    private Order createOrderEntity(Long userId, String address, OrderRequest orderRequest, BigDecimal total) {
        Order order = new Order();
        order.setUserId(userId);
        order.setAddress(address);
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PENDING);
        // QUAN TRỌNG: Khởi tạo list để tránh NullPointerException
        order.setItems(new ArrayList<>());
        return order;
    }

    private List<OrderItemResponse> processOrderItems(Order order, OrderRequest request, Map<Long, Product> productMap) {
        List<OrderItemResponse> responseList = new ArrayList<>();

        for (var item : request.getItems()) {
            Product product = productMap.get(item.getProductId());

            // 1. Trừ kho (Chưa lưu vội)
            product.setStock(product.getStock() - item.getQuantity());

            // 2. Tạo OrderItem Entity
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setNote(item.getNote());

            // Add vào Order cha
            order.getItems().add(orderItem);

            // 3. Tạo Response DTO (Để gửi cho Frontend và RabbitMQ)
            ProductResponse productResponse = ProductResponse.builder()
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .note(item.getNote())
                    .build();

            OrderItemResponse response = OrderItemResponse.builder()
                    .quantity(item.getQuantity())
                    .price(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))) // Tổng tiền của line item này
                    .products(productResponse) // Cái này gửi sang Shipment service sẽ hứng được
                    .build();

            responseList.add(response);
        }
        return responseList;
    }
}