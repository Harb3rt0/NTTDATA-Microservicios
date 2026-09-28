package tacos.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

//TC-12 - Contrato minimo y seguro para cocina
@Data
public class KitchenOrderEvent implements Serializable {
  private static final long serialVersionUID = 1L;

  private String orderId;
  private Date placedAt;
  private String deliveryName;
  private String deliveryStreet;
  private String deliveryCity;
  private String deliveryState;
  private String deliveryZip;
  private List<KitchenTaco> tacos = new ArrayList<>();

  @Data
  public static class KitchenTaco implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<KitchenIngredient> ingredients = new ArrayList<>();
  }

  @Data
  public static class KitchenIngredient implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
  }
}
//Fin TC-12
