package tacos;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

//TC-25 - Auditoria de transiciones de estado
@Data
public class OrderStatusHistory implements Serializable {
  private static final long serialVersionUID = 1L;

  private OrderStatus from;
  private OrderStatus to;
  private String actor;
  private Date occurredAt;
  private String origin;
  private String reason;

  public OrderStatusHistory() {
  }

  public OrderStatusHistory(OrderStatus from, OrderStatus to, String actor,
      Date occurredAt, String origin, String reason) {
    this.from = from;
    this.to = to;
    this.actor = actor;
    this.occurredAt = occurredAt;
    this.origin = origin;
    this.reason = reason;
  }
}
//Fin TC-25
