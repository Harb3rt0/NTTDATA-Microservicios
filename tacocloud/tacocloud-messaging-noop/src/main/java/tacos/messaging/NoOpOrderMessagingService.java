package tacos.messaging;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "noop", matchIfMissing = true)
@Slf4j
public class NoOpOrderMessagingService
       implements OrderMessagingService {
  
  public void sendOrder(OrderEvent order) { //modificacion para TC-27
    log.info("Sending event {} for order {} to noop transport", order.getEventId(),
        order.getPayload().getOrderId());
  }
  
}
