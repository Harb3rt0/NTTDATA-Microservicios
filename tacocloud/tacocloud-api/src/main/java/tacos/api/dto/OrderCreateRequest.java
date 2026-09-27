package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateRequest {
    private String deliveryName;
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZip;
    private String ccNumber;
    private String ccExpiration;
    private String ccCVV;
    private List<TacoCreateRequest> tacos = new ArrayList<>();
}
