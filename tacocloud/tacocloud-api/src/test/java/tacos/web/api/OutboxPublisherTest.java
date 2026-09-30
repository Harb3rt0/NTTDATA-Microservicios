package tacos.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OutboxEvent;
import tacos.OutboxStatus;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;
import tacos.messaging.OrderMessagingService;

//TC-29 - Reintentos, recuperacion y estado final del outbox
public class OutboxPublisherTest {
  @Test
  public void shouldPublishAndMarkEventAsPublished() throws Exception {
    ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    OrderMessagingService messaging = mock(OrderMessagingService.class);
    OutboxEvent claimed = event(0);
    OutboxEvent published = event(0);
    published.setStatus(OutboxStatus.PUBLISHED);
    stub(template, claimed, published);

    StepVerifier.create(publisher(template, messaging, 5).publishBatch())
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OutboxStatus.PUBLISHED))
        .verifyComplete();
    verify(messaging).sendOrder(any(OrderEvent.class));
  }

  @Test
  public void shouldKeepBrokerFailureRetryable() throws Exception {
    ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    OrderMessagingService messaging = mock(OrderMessagingService.class);
    doThrow(new IllegalStateException("broker unavailable"))
        .when(messaging).sendOrder(any(OrderEvent.class));
    OutboxEvent claimed = event(0);
    OutboxEvent retryable = event(1);
    retryable.setStatus(OutboxStatus.NEW);
    stub(template, claimed, retryable);

    StepVerifier.create(publisher(template, messaging, 3).publishBatch())
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OutboxStatus.NEW))
        .verifyComplete();
  }

  @Test
  public void shouldMarkFailedWhenAttemptsAreExhausted() throws Exception {
    ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    OrderMessagingService messaging = mock(OrderMessagingService.class);
    doThrow(new IllegalStateException("broker unavailable"))
        .when(messaging).sendOrder(any(OrderEvent.class));
    OutboxEvent claimed = event(2);
    OutboxEvent failed = event(3);
    failed.setStatus(OutboxStatus.FAILED);
    stub(template, claimed, failed);

    StepVerifier.create(publisher(template, messaging, 3).publishBatch())
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(OutboxStatus.FAILED))
        .verifyComplete();
  }

  private OutboxPublisher publisher(ReactiveMongoTemplate template,
      OrderMessagingService messaging, int maxAttempts) {
    return new OutboxPublisher(template, messaging, new ObjectMapper(), 2, maxAttempts,
        Duration.ofMillis(1), Duration.ofMinutes(1));
  }

  private void stub(ReactiveMongoTemplate template, OutboxEvent claimed, OutboxEvent result) {
    when(template.findAndModify(any(Query.class), any(Update.class),
        any(FindAndModifyOptions.class), eq(OutboxEvent.class)))
        .thenReturn(Mono.just(claimed), Mono.just(result), Mono.empty());
  }

  private OutboxEvent event(int attempts) throws Exception {
    OrderEventPayload payload = new OrderEventPayload();
    payload.setOrderId("ORDER-1");
    payload.setStatus("CREATED");
    OrderEvent orderEvent = new OrderEvent();
    orderEvent.setEventId("EVENT-1");
    orderEvent.setCorrelationId("CORR-1");
    orderEvent.setType(OrderEventType.CREATED);
    orderEvent.setVersion(1);
    orderEvent.setPayload(payload);

    OutboxEvent outbox = new OutboxEvent();
    outbox.setId("OUTBOX-1");
    outbox.setEventId("EVENT-1");
    outbox.setStatus(OutboxStatus.PUBLISHING);
    outbox.setAttempts(attempts);
    outbox.setCreatedAt(new Date());
    outbox.setPayload(new ObjectMapper().writeValueAsString(orderEvent));
    return outbox;
  }
}
//Fin TC-29
