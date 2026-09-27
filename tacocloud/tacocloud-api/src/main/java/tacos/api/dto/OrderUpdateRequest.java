package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderUpdateRequest {
    private String deliveryName;
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZip;
    private List<TacoCreateRequest> tacos = new ArrayList<>();
}
