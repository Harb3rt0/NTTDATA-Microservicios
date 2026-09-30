package tacos.api.dto;

import java.util.Date;

import lombok.Data;
import tacos.OrderStatus;

//TC-25 - Auditoria segura para respuesta HTTP
@Data
public class OrderStatusHistoryResponse {
  private OrderStatus from;
  private OrderStatus to;
  private String actor;
  private Date occurredAt;
  private String origin;
  private String reason;
}
//Fin TC-25
