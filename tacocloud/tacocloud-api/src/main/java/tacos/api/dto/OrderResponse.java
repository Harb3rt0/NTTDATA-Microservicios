package tacos.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data 
public class OrderResponse {
    private String id;
    private Date placedAt;
    private String deliveryName;
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZip;
    //TC-14 - Totales y lineas seguras de la orden
    private List<OrderLineResponse> items = new ArrayList<>();
    private BigDecimal total;
    private String currency;
    //Fin TC-14
}
