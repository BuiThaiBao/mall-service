package vn.vti.dtn2504.mallservice.dto.response;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String fullName;
    private String email;
    private String orderId;
    private String phoneNumber;
    private BigDecimal totalPrice;
    private String orderStatus;
    private String address;

    private List<OrderItemResponse> orderItems;
}
