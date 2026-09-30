package tacos.api.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import tacos.OrderStatus;

//TC-25 - Entrada controlada para una transicion
@Data
public class OrderStatusRequest {
  @NotNull
  private OrderStatus status;

  @Size(max = 200)
  private String reason;
}
//Fin TC-25
