package tacos.api.mapper;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.OrderLine;
import tacos.TacoOrder;
import tacos.messaging.OrderEvent;
import tacos.messaging.KitchenOrderEvent;
import tacos.messaging.OrderEventItem;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

//TC-27 - Mapper de entidad Mongo al contrato seguro versionado
@Component
public class KitchenOrderEventMapper {

    public KitchenOrderEvent toEvent(TacoOrder order) {
        return (KitchenOrderEvent) toEvent(order, OrderEventType.CREATED, UUID.randomUUID().toString());
    }

    public OrderEvent toEvent(TacoOrder order, OrderEventType type, String correlationId) {
        OrderEventPayload payload = new OrderEventPayload();
        payload.setOrderId(order.getId());
        payload.setPlacedAt(order.getPlacedAt());
        payload.setStatus(order.getStatus() == null ? null : order.getStatus().name());
        payload.setItems(order.getItems().stream().map(this::toItem).collect(Collectors.toList()));

        KitchenOrderEvent event = new KitchenOrderEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(type);
        event.setVersion(1);
        event.setOccurredAt(new Date());
        event.setCorrelationId(correlationId);
        event.setPayload(payload);
        payload.getItems().forEach(item -> {
            KitchenOrderEvent.KitchenTaco taco = new KitchenOrderEvent.KitchenTaco();
            taco.setQuantity(item.getQuantity());
            event.getTacos().add(taco);
        });
        return event;
    }

    private OrderEventItem toItem(OrderLine line) {
        OrderEventItem item = new OrderEventItem();
        item.setName(line.getTaco().getName());
        item.setQuantity(line.getQuantity());
        item.setIngredients(line.getTaco().getIngredients().stream()
            .map(ingredient -> ingredient.getName()).collect(Collectors.toList()));
        return item;
    }
}
//Fin TC-27
