package tacos.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "kafka")
public class KafkaOrderMessagingService
                                  implements OrderMessagingService {
  
  private KafkaTemplate<String, OrderEvent> kafkaTemplate;
  private final String destination;
  
  @Autowired
  public KafkaOrderMessagingService(
          KafkaTemplate<String, OrderEvent> kafkaTemplate,
          @Value("${tacocloud.messaging.destination:tacocloud.orders.topic}") String destination) {
    this.kafkaTemplate = kafkaTemplate;
    this.destination = destination;
  }
  
  @Override
  public void sendOrder(OrderEvent order) { //modificacion para TC-27
    kafkaTemplate.send(destination, order);
  }
  
}
