package tacos.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

//TC-30 - Idempotencia y version desconocida
public class KitchenEventProcessorTest {
  @Test
  public void shouldApplyEffectAndMarkerOnlyOnce() {
    ProcessedEventRepository processed = Mockito.mock(ProcessedEventRepository.class);
    KitchenOrderViewRepository orders = Mockito.mock(KitchenOrderViewRepository.class);
    when(processed.existsByEventId("E1")).thenReturn(false, true);
    when(orders.findById("O1")).thenReturn(Optional.empty());
    KitchenEventProcessor processor = new KitchenEventProcessor(processed, orders);
    OrderEvent event = event(1);

    assertThat(processor.process(event)).isTrue();
    assertThat(processor.process(event)).isFalse();

    verify(orders).save(any(KitchenOrderView.class));
    verify(processed).save(any(ProcessedEvent.class));
  }

  @Test
  public void shouldRejectUnknownVersionWithoutEffect() {
    ProcessedEventRepository processed = Mockito.mock(ProcessedEventRepository.class);
    KitchenOrderViewRepository orders = Mockito.mock(KitchenOrderViewRepository.class);
    KitchenEventProcessor processor = new KitchenEventProcessor(processed, orders);

    org.junit.jupiter.api.Assertions.assertThrows(PermanentEventException.class,
        () -> processor.process(event(2)));
    verify(orders, never()).save(any(KitchenOrderView.class));
  }

  @Test
  public void shouldRejectUnknownEventTypeWithoutEffect() {
    ProcessedEventRepository processed = Mockito.mock(ProcessedEventRepository.class);
    KitchenOrderViewRepository orders = Mockito.mock(KitchenOrderViewRepository.class);
    KitchenEventProcessor processor = new KitchenEventProcessor(processed, orders);
    OrderEvent event = event(1);
    event.setType(null);

    org.junit.jupiter.api.Assertions.assertThrows(PermanentEventException.class,
        () -> processor.process(event));
    verify(orders, never()).save(any(KitchenOrderView.class));
    verify(processed, never()).save(any(ProcessedEvent.class));
  }

  private OrderEvent event(int version) {
    OrderEventPayload payload = new OrderEventPayload();
    payload.setOrderId("O1");
    payload.setStatus("CREATED");
    OrderEvent event = new OrderEvent();
    event.setEventId("E1");
    event.setType(OrderEventType.CREATED);
    event.setVersion(version);
    event.setPayload(payload);
    return event;
  }
}
//Fin TC-30
