package vn.vti.dtn2504.mallservice.dto.request;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OrderItemRequest {
    private Long productId;
    private Integer quantity;
    private String note;
}
