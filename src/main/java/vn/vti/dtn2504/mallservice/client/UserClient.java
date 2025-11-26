package vn.vti.dtn2504.mallservice.client;


import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "user-manager"
)
public interface UserClient {
}
