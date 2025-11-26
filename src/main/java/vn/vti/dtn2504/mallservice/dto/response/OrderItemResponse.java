package vn.vti.dtn2504.mallservice.dto.response;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class OrderItemResponse {
    Integer quantity;
    BigDecimal price;
    ProductResponse products;
}
