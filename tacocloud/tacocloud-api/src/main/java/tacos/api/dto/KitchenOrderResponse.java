package tacos.api.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;
import tacos.OrderStatus;

//TC-26 - Vista operativa sin pago, direccion ni datos internos
@Data
public class KitchenOrderResponse {
  private String orderId;
  private Date placedAt;
  private OrderStatus status;
  private String stationId;
  private long estimatedMinutes;
  private List<KitchenOrderItemResponse> items = new ArrayList<>();
}
//Fin TC-26
