package vn.vti.dtn2504.mallservice.service;



import vn.vti.dtn2504.mallservice.dto.request.OrderRequest;
import vn.vti.dtn2504.mallservice.dto.response.OrderResponse;

public interface OrderService {
    public OrderResponse createOrder(Long userId,String fullName, String email, String address,String phoneNumber, OrderRequest request);
}
