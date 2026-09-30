package tacos.web.api;

import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.OutboxEvent;
import tacos.OutboxStatus;
import tacos.TacoOrder;
import tacos.api.mapper.KitchenOrderEventMapper;
import tacos.data.OutboxEventRepository;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventType;

//TC-29 - Crea el evento durable dentro de la transaccion de la orden
@Service
public class OrderOutboxService {
  private final OutboxEventRepository outboxRepo;
  private final KitchenOrderEventMapper eventMapper;
  private final ObjectMapper objectMapper;

  public OrderOutboxService(OutboxEventRepository outboxRepo,
      KitchenOrderEventMapper eventMapper, ObjectMapper objectMapper) {
    this.outboxRepo = outboxRepo;
    this.eventMapper = eventMapper;
    this.objectMapper = objectMapper;
  }

  public Mono<OutboxEvent> record(TacoOrder order, OrderEventType type, String correlationId) {
    return Mono.defer(() -> {
      OrderEvent event = eventMapper.toEvent(order, type, correlationId);
      Date now = new Date();
      OutboxEvent outbox = new OutboxEvent();
      outbox.setEventId(event.getEventId());
      outbox.setEventType(event.getType().name());
      outbox.setEventVersion(event.getVersion());
      outbox.setAggregateId(order.getId());
      outbox.setCorrelationId(event.getCorrelationId());
      outbox.setStatus(OutboxStatus.NEW);
      outbox.setCreatedAt(now);
      outbox.setUpdatedAt(now);
      outbox.setNextAttemptAt(now);
      try {
        outbox.setPayload(objectMapper.writeValueAsString(event));
      } catch (JsonProcessingException ex) {
        return Mono.error(ex);
      }
      return outboxRepo.save(outbox);
    });
  }
}
//Fin TC-29
