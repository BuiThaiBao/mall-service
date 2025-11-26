package vn.vti.dtn2504.mallservice.dto.response;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
public class ProductResponse {
    private String name;
    private String description;
    private BigDecimal price;
    private String note;
}
