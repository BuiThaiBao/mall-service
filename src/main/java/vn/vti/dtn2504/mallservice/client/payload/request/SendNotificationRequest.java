package vn.vti.dtn2504.mallservice.client.payload.request;


import lombok.*;
import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SendNotificationRequest {
    private String sendTo;
    private String message;
    private String title;
    private String templateCode;

    private Map<String,Object> param;


}
