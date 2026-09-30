package tacos.kitchen.messaging.rabbit.listener;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tacos.kitchen.KitchenEventProcessor;
import tacos.kitchen.KitchenUI;
import tacos.kitchen.PermanentEventException;
import tacos.messaging.OrderEvent;

//TC-30 - Consumidor Rabbit idempotente con retry y DLQ
@Profile("rabbitmq-listener")
@Component
public class OrderListener {
  private static final String RETRY_HEADER = "x-tacocloud-retry";

  private final KitchenEventProcessor processor;
  private final KitchenUI ui;
  private final RabbitTemplate rabbitTemplate;
  private final String queue;
  private final String retryQueue;
  private final int maxAttempts;
  private final Counter processed;
  private final Counter duplicates;
  private final Counter failed;
  private final Counter deadLetters;

  public OrderListener(KitchenEventProcessor processor, KitchenUI ui, RabbitTemplate rabbitTemplate,
      MeterRegistry registry,
      @Value("${tacocloud.messaging.destination:tacocloud.order.queue}") String queue,
      @Value("${tacocloud.consumer.max-attempts:3}") int maxAttempts) {
    this.processor = processor;
    this.ui = ui;
    this.rabbitTemplate = rabbitTemplate;
    this.queue = queue;
    this.retryQueue = queue + ".retry";
    this.maxAttempts = maxAttempts;
    this.processed = registry.counter("tacocloud.kitchen.events.processed");
    this.duplicates = registry.counter("tacocloud.kitchen.events.duplicates");
    this.failed = registry.counter("tacocloud.kitchen.events.failed");
    this.deadLetters = registry.counter("tacocloud.kitchen.events.deadlettered");
  }

  @RabbitListener(queues = "${tacocloud.messaging.destination:tacocloud.order.queue}")
  public void receiveOrder(OrderEvent event, Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
      if (processor.process(event)) {
        processed.increment();
        ui.displayOrder(event);
      } else {
        duplicates.increment();
      }
      channel.basicAck(deliveryTag, false);
    } catch (PermanentEventException error) {
      failed.increment();
      deadLetter(event, "PERMANENT");
      channel.basicAck(deliveryTag, false);
    } catch (RuntimeException error) {
      failed.increment();
      int attempt = retryCount(message) + 1;
      if (attempt >= maxAttempts) {
        deadLetter(event, "RETRIES_EXHAUSTED");
      } else {
        rabbitTemplate.convertAndSend("", retryQueue, event, outbound -> {
          outbound.getMessageProperties().setHeader(RETRY_HEADER, attempt);
          outbound.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
          return outbound;
        });
      }
      channel.basicAck(deliveryTag, false);
    }
  }

  private int retryCount(Message message) {
    Object value = message.getMessageProperties().getHeaders().get(RETRY_HEADER);
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  private void deadLetter(OrderEvent event, String reason) {
    rabbitTemplate.convertAndSend("tacocloud.order.dlx", "tacocloud.order.dlq", event, outbound -> {
      outbound.getMessageProperties().setHeader("x-failure-reason", reason);
      outbound.getMessageProperties().setHeader("x-original-event-id", event.getEventId());
      outbound.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
      return outbound;
    });
    deadLetters.increment();
  }
}
//Fin TC-30
