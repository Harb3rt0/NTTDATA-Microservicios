package tacos.kitchen;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

//TC-30 - Roundtrip real del contrato elegido en RabbitMQ
@Testcontainers(disabledWithoutDocker = true)
public class Tc30RabbitContractE2eTest {
  @Container
  private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.11-management");

  @Test
  public void shouldDeliverVersionedEventThroughRealBroker() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri(RABBIT.getAmqpUrl());
    ObjectMapper mapper = new ObjectMapper();
    OrderEventPayload payload = new OrderEventPayload();
    payload.setOrderId("O-E2E");
    payload.setStatus("CREATED");
    OrderEvent event = new OrderEvent();
    event.setEventId("E-E2E");
    event.setType(OrderEventType.CREATED);
    event.setVersion(1);
    event.setPayload(payload);

    try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
      channel.queueDeclare("tc30.e2e", false, false, true, null);
      channel.basicPublish("", "tc30.e2e", null, mapper.writeValueAsBytes(event));
      GetResponse received = channel.basicGet("tc30.e2e", true);
      OrderEvent decoded = mapper.readValue(
          new String(received.getBody(), StandardCharsets.UTF_8), OrderEvent.class);
      assertThat(decoded.getEventId()).isEqualTo("E-E2E");
      assertThat(decoded.getPayload().getOrderId()).isEqualTo("O-E2E");
    }
  }
}
//Fin TC-30
