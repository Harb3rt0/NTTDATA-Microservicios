package tacos.kitchen;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tacos.messaging.OrderEvent;

//TC-30 - Efecto y marcador coordinados en una transaccion Mongo
@Service
public class KitchenEventProcessor {
  private final ProcessedEventRepository processedRepo;
  private final KitchenOrderViewRepository kitchenOrderRepo;

  public KitchenEventProcessor(ProcessedEventRepository processedRepo,
      KitchenOrderViewRepository kitchenOrderRepo) {
    this.processedRepo = processedRepo;
    this.kitchenOrderRepo = kitchenOrderRepo;
  }

  @Transactional
  public boolean process(OrderEvent event) {
    validate(event);
    if (processedRepo.existsByEventId(event.getEventId())) {
      return false;
    }
    KitchenOrderView view = kitchenOrderRepo.findById(event.getPayload().getOrderId())
        .orElseGet(KitchenOrderView::new);
    view.setOrderId(event.getPayload().getOrderId());
    view.setStatus(event.getPayload().getStatus());
    view.setItems(event.getPayload().getItems());
    view.setUpdatedAt(new Date());
    kitchenOrderRepo.save(view);

    ProcessedEvent processed = new ProcessedEvent();
    processed.setEventId(event.getEventId());
    processed.setEventType(event.getType().name());
    processed.setProcessedAt(new Date());
    processedRepo.save(processed);
    return true;
  }

  private void validate(OrderEvent event) {
    if (event == null || event.getEventId() == null || event.getType() == null
        || event.getPayload() == null || event.getPayload().getOrderId() == null) {
      throw new PermanentEventException("Malformed order event.");
    }
    if (event.getVersion() != 1) {
      throw new PermanentEventException("Unsupported order event version.");
    }
  }
}
//Fin TC-30
