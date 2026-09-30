package tacos.messaging.jms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import tacos.messaging.OrderEvent;

//TC-28 - Configuracion JMS unica y condicional
@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "jms")
public class JmsMessagingConfig {
  @Bean
  public MappingJackson2MessageConverter jmsOrderMessageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTypeIdPropertyName("_typeId");
    Map<String, Class<?>> mappings = new HashMap<>();
    mappings.put("orderEvent", OrderEvent.class);
    converter.setTypeIdMappings(mappings);
    return converter;
  }
}
//Fin TC-28
