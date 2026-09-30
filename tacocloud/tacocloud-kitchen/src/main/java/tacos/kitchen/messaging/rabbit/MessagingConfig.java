package tacos.kitchen.messaging.rabbit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"rabbitmq-template", "rabbitmq-listener"})
@Configuration
public class MessagingConfig {

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    return new Jackson2JsonMessageConverter(mapper);
  }

  //TC-30 - Topologia durable, DLQ y ack manual
  @Bean
  public Queue orderQueue(@Value("${tacocloud.messaging.destination:tacocloud.order.queue}") String name) {
    return org.springframework.amqp.core.QueueBuilder.durable(name)
        .withArgument("x-dead-letter-exchange", "tacocloud.order.dlx").build();
  }

  @Bean
  public DirectExchange orderDeadLetterExchange() {
    return new DirectExchange("tacocloud.order.dlx", true, false);
  }

  @Bean
  public Queue orderDeadLetterQueue() {
    return new Queue("tacocloud.order.dlq", true);
  }

  @Bean
  public Queue orderRetryQueue(
      @Value("${tacocloud.messaging.destination:tacocloud.order.queue}") String destination,
      @Value("${tacocloud.consumer.retry-delay-ms:2000}") int retryDelayMs) {
    return org.springframework.amqp.core.QueueBuilder.durable(destination + ".retry")
        .withArgument("x-message-ttl", retryDelayMs)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", destination).build();
  }

  @Bean
  public Binding orderDeadLetterBinding(Queue orderDeadLetterQueue,
      DirectExchange orderDeadLetterExchange) {
    return BindingBuilder.bind(orderDeadLetterQueue).to(orderDeadLetterExchange)
        .with("tacocloud.order.dlq");
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
  //Fin TC-30

}
