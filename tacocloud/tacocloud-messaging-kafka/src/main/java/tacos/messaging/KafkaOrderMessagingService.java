package tacos.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaOrderMessagingService
                                  implements OrderMessagingService {
  
  private KafkaTemplate<String, KitchenOrderEvent> kafkaTemplate; //modificacion para TC-12
  
  @Autowired
  public KafkaOrderMessagingService(
          KafkaTemplate<String, KitchenOrderEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }
  
  @Override
  public void sendOrder(KitchenOrderEvent order) { //modificacion para TC-12
    kafkaTemplate.send("tacocloud.orders.topic", order);
  }
  
}
