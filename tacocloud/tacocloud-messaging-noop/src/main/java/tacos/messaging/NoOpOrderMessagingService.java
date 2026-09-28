package tacos.messaging;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NoOpOrderMessagingService
       implements OrderMessagingService {
  
  public void sendOrder(KitchenOrderEvent order) { //modificacion para TC-12
    log.info("Sending order {} to kitchen with {} tacos", order.getOrderId(), order.getTacos().size());
  }
  
}
