package tacos.messaging;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

//TC-27 - Vista de compatibilidad ubicada solo en el modulo de contrato
@Deprecated
public class KitchenOrderEvent extends OrderEvent {
  private final List<KitchenTaco> tacos = new ArrayList<>();
  private String legacyOrderId;
  private String legacyDeliveryStreet;

  @JsonIgnore
  public String getOrderId() {
    return getPayload() == null ? legacyOrderId : getPayload().getOrderId();
  }

  public void setOrderId(String orderId) {
    this.legacyOrderId = orderId;
    if (getPayload() == null) {
      setPayload(new OrderEventPayload());
    }
    getPayload().setOrderId(orderId);
  }

  @JsonIgnore
  public String getDeliveryStreet() {
    return legacyDeliveryStreet;
  }

  public void setDeliveryStreet(String deliveryStreet) {
    this.legacyDeliveryStreet = deliveryStreet;
  }

  @JsonIgnore
  public List<KitchenTaco> getTacos() {
    return tacos;
  }

  public static class KitchenTaco {
    private int quantity;
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
  }
}
//Fin TC-27
