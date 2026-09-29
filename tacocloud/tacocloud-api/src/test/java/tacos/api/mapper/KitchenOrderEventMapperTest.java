package tacos.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.Ingredient;
import tacos.OrderLine;
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
        OrderLine item = new OrderLine(); //modificacion para TC-14
        item.setTaco(taco);
        item.setQuantity(3);
        item.setUnitPriceAtPurchase(new BigDecimal("1.25"));
        item.setSubtotal(new BigDecimal("3.75"));
        order.setItems(Collections.singletonList(item));
        order.setTotal(new BigDecimal("3.75"));
        order.setCurrency("USD");

        KitchenOrderEvent event = new KitchenOrderEventMapper().toEvent(order);
        String json = new ObjectMapper().writeValueAsString(event);

        assertThat(json).contains("ORDER1", "Lab taco", "Flour Tortilla");
        assertThat(event.getTacos().get(0).getQuantity()).isEqualTo(3); //modificacion para TC-14
        assertThat(json.toLowerCase()).doesNotContain("payment", "token", "card", "cvv",
            "user", "password", "alice@example.com", "subtotal", "unitprice", "currency");
    }
}
//Fin TC-12
