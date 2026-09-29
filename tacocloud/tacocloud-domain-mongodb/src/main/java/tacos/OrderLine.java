package tacos;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

//TC-14 - Snapshot economico de una linea de pedido
@Data
public class OrderLine implements Serializable {
  private static final long serialVersionUID = 1L;

  private Taco taco;
  private int quantity;
  private BigDecimal unitPriceAtPurchase;
  private BigDecimal subtotal;
}
//Fin TC-14
