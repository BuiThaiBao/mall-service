package vn.vti.dtn2504.mallservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.vti.dtn2504.common.api.response.ApiResponse;
import vn.vti.dtn2504.mallservice.dto.request.OrderRequest;
import vn.vti.dtn2504.mallservice.dto.response.OrderResponse;
import vn.vti.dtn2504.mallservice.service.OrderService;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@AuthenticationPrincipal Jwt jwt, @RequestBody OrderRequest request) {
        log.info("Received request to create order with payload: {}", request);
        Long userId = jwt.getClaim("userId");

        if (userId == null) {
            log.warn("Cannot create order because userId is missing from the JWT token.");
            throw new RuntimeException("Không tìm thấy userId trong token");
        }
        log.info("Processing order creation for userId: {}", userId);

        try {
            String email = jwt.getClaim("email");
            String fullName = jwt.getClaim("fullName");
            String address = jwt.getClaim("address");
            String phoneNumber = jwt.getClaim("phoneNumber");

            OrderResponse response = orderService.createOrder(userId, fullName, email, address, phoneNumber, request);

            log.info("Successfully created order with ID: {} for userId: {}", response.getOrderId(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to create order for userId: {}. Error: {}", userId, e.getMessage(), e);
            // Re-throwing the exception to allow a global exception handler to process it.
            // This preserves the original behavior.
            throw e;
        }
    }
}
