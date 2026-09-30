package tacos.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//TC-27 - Payload seguro e independiente de Mongo
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEventPayload implements Serializable {
  private static final long serialVersionUID = 1L;

  private String orderId;
  private String status;
  private Date placedAt;
  private List<OrderEventItem> items = new ArrayList<>();

  public String getOrderId() { return orderId; }
  public void setOrderId(String orderId) { this.orderId = orderId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Date getPlacedAt() { return placedAt; }
  public void setPlacedAt(Date placedAt) { this.placedAt = placedAt; }
  public List<OrderEventItem> getItems() { return items; }
  public void setItems(List<OrderEventItem> items) { this.items = items; }
}
//Fin TC-27
