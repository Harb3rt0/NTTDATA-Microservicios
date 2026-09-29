package tacos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@Document
public class TacoOrder implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  private String id;
  private Date placedAt = new Date();

  private User user;

  private String deliveryName;

  private String deliveryStreet;

  private String deliveryCity;

  private String deliveryState;

  private String deliveryZip;

  @JsonIgnore
  @ToString.Exclude
  private String paymentMethodId; //modificacion para TC-12

  //TC-14 - Lineas e importes calculados por el servidor
  private List<OrderLine> items = new ArrayList<>();
  private BigDecimal total = new BigDecimal("0.00");
  private String currency;

  public void addItem(OrderLine item) {
    this.items.add(item);
  }
  //Fin TC-14

}
