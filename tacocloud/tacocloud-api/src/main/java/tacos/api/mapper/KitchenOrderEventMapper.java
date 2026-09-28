package tacos.api.mapper;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.messaging.KitchenOrderEvent;
import tacos.messaging.KitchenOrderEvent.KitchenIngredient;
import tacos.messaging.KitchenOrderEvent.KitchenTaco;

//TC-12 - Mapper de entidad Mongo a evento seguro de cocina
@Component
public class KitchenOrderEventMapper {

    public KitchenOrderEvent toEvent(TacoOrder order) {
        KitchenOrderEvent event = new KitchenOrderEvent();
        event.setOrderId(order.getId());
        event.setPlacedAt(order.getPlacedAt());
        event.setDeliveryName(order.getDeliveryName());
        event.setDeliveryStreet(order.getDeliveryStreet());
        event.setDeliveryCity(order.getDeliveryCity());
        event.setDeliveryState(order.getDeliveryState());
        event.setDeliveryZip(order.getDeliveryZip());

        order.getTacos().forEach(taco -> event.getTacos().add(toTaco(taco)));
        return event;
    }

    private KitchenTaco toTaco(Taco taco) {
        KitchenTaco kitchenTaco = new KitchenTaco();
        kitchenTaco.setName(taco.getName());
        taco.getIngredients().forEach(ingredient ->
            kitchenTaco.getIngredients().add(toIngredient(ingredient)));
        return kitchenTaco;
    }

    private KitchenIngredient toIngredient(Ingredient ingredient) {
        KitchenIngredient kitchenIngredient = new KitchenIngredient();
        kitchenIngredient.setName(ingredient.getName());
        kitchenIngredient.setType(ingredient.getType().name());
        return kitchenIngredient;
    }
}
//Fin TC-12
