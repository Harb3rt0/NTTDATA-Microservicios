package tacos.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.Ingredient;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.messaging.KitchenOrderEvent;

//TC-12 - Evento de cocina sin usuario ni informacion de pago
public class KitchenOrderEventMapperTest {

    @Test
    public void shouldSerializeKitchenEventWithoutPaymentOrUserData() throws Exception {
        User user = new User("alice", "{bcrypt}hash", "Test User", "Private Street",
            "City", "TX", "78701", "555-0100", "alice@example.com");
        Taco taco = new Taco();
        taco.setName("Lab taco");
        taco.setIngredients(Collections.singletonList(
            new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP)));
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");
        order.setUser(user);
        order.setPaymentMethodId("PAYMENT1");
        order.setTacos(Collections.singletonList(taco));

        KitchenOrderEvent event = new KitchenOrderEventMapper().toEvent(order);
        String json = new ObjectMapper().writeValueAsString(event);

        assertThat(json).contains("ORDER1", "Lab taco", "Flour Tortilla");
        assertThat(json.toLowerCase()).doesNotContain("payment", "token", "card", "cvv",
            "user", "password", "alice@example.com");
    }
}
//Fin TC-12
