package tacos.kitchen;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tacos.messaging.OrderEvent;

@Component
@Slf4j
public class KitchenUI {

  public void displayOrder(OrderEvent order) { //modificacion para TC-30
    log.info("Received event {} for order {} with {} items", order.getEventId(),
        order.getPayload().getOrderId(), order.getPayload().getItems().size());
  }
  
}
