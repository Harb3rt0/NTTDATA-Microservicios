package tacos.kitchen;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tacos.messaging.KitchenOrderEvent;

@Component
@Slf4j
public class KitchenUI {

  public void displayOrder(KitchenOrderEvent order) { //modificacion para TC-12
    // TODO: Beef this up to do more than just log the received taco.
    //       To display it in some sort of UI.
    log.info("Received order {} with {} tacos", order.getOrderId(), order.getTacos().size());
  }
  
}
