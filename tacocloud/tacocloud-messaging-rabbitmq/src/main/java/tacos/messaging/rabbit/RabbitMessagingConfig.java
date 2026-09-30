package tacos.messaging.rabbit;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//TC-28 - Configuracion Rabbit unica y condicional
@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "rabbit")
public class RabbitMessagingConfig {
  @Bean
  public Jackson2JsonMessageConverter rabbitOrderMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
//Fin TC-28
