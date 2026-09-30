package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import tacos.messaging.JmsOrderMessagingService;
import tacos.messaging.KafkaOrderMessagingService;
import tacos.messaging.NoOpOrderMessagingService;
import tacos.messaging.OrderMessagingService;
import tacos.messaging.RabbitOrderMessagingService;

//TC-28 - Seleccion exclusiva sin brokers reales
public class Tc28MessagingTransportConfigurationTest {
  @Test
  public void shouldSelectNoop() {
    runner(NoOpOrderMessagingService.class, "noop").run(context ->
        assertThat(context).hasSingleBean(OrderMessagingService.class));
  }

  @Test
  public void shouldSelectJms() {
    runner(JmsOrderMessagingService.class, "jms")
        .withBean(JmsTemplate.class, () -> mock(JmsTemplate.class))
        .run(context -> assertThat(context).hasSingleBean(OrderMessagingService.class));
  }

  @Test
  public void shouldSelectRabbit() {
    runner(RabbitOrderMessagingService.class, "rabbit")
        .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
        .run(context -> assertThat(context).hasSingleBean(OrderMessagingService.class));
  }

  @Test
  public void shouldSelectKafka() {
    runner(KafkaOrderMessagingService.class, "kafka")
        .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
        .run(context -> assertThat(context).hasSingleBean(OrderMessagingService.class));
  }

  @Test
  public void shouldFailForInvalidTransport() {
    new ApplicationContextRunner().withUserConfiguration(MessagingTransportConfiguration.class)
        .withPropertyValues("tacocloud.messaging.transport=invalid")
        .run(context -> assertThat(context).hasFailed());
  }

  private ApplicationContextRunner runner(Class<?> adapter, String transport) {
    return new ApplicationContextRunner().withUserConfiguration(adapter)
        .withPropertyValues("tacocloud.messaging.transport=" + transport,
            "tacocloud.messaging.destination=test.destination");
  }
}
//Fin TC-28
