package tacos.kitchen;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tacos.messaging.OrderEventItem;

//TC-30 - Efecto durable y seguro del consumidor
@Data
@Document(collection = "kitchen_orders")
public class KitchenOrderView {
  @Id
  private String orderId;
  private String status;
  private List<OrderEventItem> items = new ArrayList<>();
  private Date updatedAt;
}
//Fin TC-30
