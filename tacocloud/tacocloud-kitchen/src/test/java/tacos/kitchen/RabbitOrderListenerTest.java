package tacos.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import tacos.kitchen.messaging.rabbit.listener.OrderListener;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

//TC-30 - Ack durable, retry acotado, DLQ y replay idempotente
public class RabbitOrderListenerTest {
  @Test
  public void shouldAckOnlyAfterDurableProcessing() throws Exception {
    Fixture fixture = fixture(3);
    when(fixture.processor.process(fixture.event)).thenReturn(true);

    fixture.listener.receiveOrder(fixture.event, message(0), fixture.channel);

    InOrder order = inOrder(fixture.processor, fixture.channel);
    order.verify(fixture.processor).process(fixture.event);
    order.verify(fixture.channel).basicAck(1L, false);
  }

  @Test
  public void shouldRetryTransientFailureWithCorrelationMetadata() throws Exception {
    Fixture fixture = fixture(3);
    when(fixture.processor.process(fixture.event)).thenThrow(new IllegalStateException("temporary"));

    fixture.listener.receiveOrder(fixture.event, message(0), fixture.channel);

    verify(fixture.rabbit).convertAndSend(eq(""), eq("orders.retry"), eq(fixture.event),
        any(MessagePostProcessor.class));
    verify(fixture.channel).basicAck(1L, false);
  }

  @Test
  public void shouldDeadLetterPermanentFailureWithSafeHeaders() throws Exception {
    Fixture fixture = fixture(3);
    when(fixture.processor.process(fixture.event))
        .thenThrow(new PermanentEventException("unsupported"));

    fixture.listener.receiveOrder(fixture.event, message(0), fixture.channel);

    ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
    verify(fixture.rabbit).convertAndSend(eq("tacocloud.order.dlx"),
        eq("tacocloud.order.dlq"), eq(fixture.event), captor.capture());
    Message outbound = captor.getValue().postProcessMessage(message(0));
    assertThat(outbound.getMessageProperties().getHeaders())
        .containsEntry("x-original-event-id", "EVENT-1")
        .containsEntry("x-correlation-id", "CORR-1")
        .doesNotContainKeys("password", "paymentToken", "cvv", "pan");
  }

  @Test
  public void shouldDeadLetterWhenRetriesAreExhausted() throws Exception {
    Fixture fixture = fixture(3);
    when(fixture.processor.process(fixture.event)).thenThrow(new IllegalStateException("temporary"));

    fixture.listener.receiveOrder(fixture.event, message(2), fixture.channel);

    verify(fixture.rabbit).convertAndSend(eq("tacocloud.order.dlx"),
        eq("tacocloud.order.dlq"), eq(fixture.event), any(MessagePostProcessor.class));
  }

  @Test
  public void shouldNotDuplicateEffectAfterAckCrashAndRedelivery() throws Exception {
    Fixture fixture = fixture(3);
    when(fixture.processor.process(fixture.event)).thenReturn(true, false);
    org.mockito.Mockito.doThrow(new IOException("connection lost")).doNothing()
        .when(fixture.channel).basicAck(1L, false);

    assertThrows(IOException.class,
        () -> fixture.listener.receiveOrder(fixture.event, message(0), fixture.channel));
    fixture.listener.receiveOrder(fixture.event, message(0), fixture.channel);

    verify(fixture.processor, times(2)).process(fixture.event);
    verify(fixture.ui, times(1)).displayOrder(fixture.event);
  }

  private Fixture fixture(int maxAttempts) {
    Fixture fixture = new Fixture();
    fixture.processor = mock(KitchenEventProcessor.class);
    fixture.ui = mock(KitchenUI.class);
    fixture.rabbit = mock(RabbitTemplate.class);
    fixture.channel = mock(Channel.class);
    fixture.event = event();
    fixture.listener = new OrderListener(fixture.processor, fixture.ui, fixture.rabbit,
        new SimpleMeterRegistry(), "orders", maxAttempts);
    return fixture;
  }

  private Message message(int retry) {
    MessageProperties properties = new MessageProperties();
    properties.setDeliveryTag(1L);
    properties.setHeader("x-tacocloud-retry", retry);
    return new Message(new byte[0], properties);
  }

  private OrderEvent event() {
    OrderEventPayload payload = new OrderEventPayload();
    payload.setOrderId("ORDER-1");
    payload.setStatus("CREATED");
    OrderEvent event = new OrderEvent();
    event.setEventId("EVENT-1");
    event.setCorrelationId("CORR-1");
    event.setType(OrderEventType.CREATED);
    event.setVersion(1);
    event.setPayload(payload);
    return event;
  }

  private static final class Fixture {
    private KitchenEventProcessor processor;
    private KitchenUI ui;
    private RabbitTemplate rabbit;
    private Channel channel;
    private OrderEvent event;
    private OrderListener listener;
  }
}
//Fin TC-30
