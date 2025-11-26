package vn.vti.dtn2504.mallservice.dto.request;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Data
@Getter
@Setter
public class OrderRequest {
    private List<OrderItemRequest> items;
}
