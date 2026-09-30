package tacos.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "rabbit")
public class RabbitOrderMessagingService
       implements OrderMessagingService {
  
  private RabbitTemplate rabbit;
  private final String destination;
  
  @Autowired
  public RabbitOrderMessagingService(RabbitTemplate rabbit,
      @Value("${tacocloud.messaging.destination:tacocloud.order.queue}") String destination) {
    this.rabbit = rabbit;
    this.destination = destination;
  }
  
  public void sendOrder(OrderEvent order) { //modificacion para TC-27
    rabbit.convertAndSend(destination, order,
        new MessagePostProcessor() {
          @Override
          public Message postProcessMessage(Message message)
              throws AmqpException {
            MessageProperties props = message.getMessageProperties();
            props.setHeader("X_ORDER_SOURCE", "WEB");
            return message;
          } 
        });
  }
  
}
