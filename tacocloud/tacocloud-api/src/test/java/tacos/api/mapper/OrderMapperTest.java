package tacos.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.Ingredient;
import tacos.OrderLine;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderResponse;

public class OrderMapperTest {
    private IngredientMapper ingredientMapper;
    private TacoMapper tacoMapper;
    private OrderMapper orderMapper;

    @BeforeEach
    public void setUp() {
        ingredientMapper =
            new IngredientMapper();

        tacoMapper =
            new TacoMapper(
                ingredientMapper
            );

        orderMapper =
            new OrderMapper(
                tacoMapper
            );
    }

    //prueba TC-08
    @Test
    public void shouldMapOrderCreateRequestToEntity() {
        OrderCreateRequest request = new OrderCreateRequest();

        request.setDeliveryName("Test User");
        request.setDeliveryStreet("Test Street");
        request.setDeliveryCity("Aguascalientes");
        request.setDeliveryState("AG");
        request.setDeliveryZip("20000");
        request.setPaymentMethodId("PAYMENT1"); //modificacion para TC-12

        Ingredient carn = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN);

        Taco taco = new Taco();
        taco.setName("Taco al pastor");
        taco.setIngredients(Arrays.asList(carn));
        OrderLine item = line(taco, 2, "1.25", "2.50");
        TacoOrder order = orderMapper.toEntity(request, Arrays.asList(item),
            new BigDecimal("2.50"), BigDecimal.ZERO, new BigDecimal("2.50"),
            null, "USD"); //modificacion para TC-15

        assertEquals("Test User", order.getDeliveryName());
        assertEquals("Test Street", order.getDeliveryStreet());
        assertEquals("Aguascalientes", order.getDeliveryCity());
        assertEquals("AG", order.getDeliveryState());
        assertEquals("20000", order.getDeliveryZip());
        assertEquals(1, order.getItems().size()); //modificacion para TC-14
        assertEquals("Taco al pastor", order.getItems().get(0).getTaco().getName());
        assertEquals(2, order.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("2.50"), order.getTotal());
        assertEquals("USD", order.getCurrency());
        assertNull(order.getId());
        assertNull(order.getUser());
        assertNotNull(order.getPlacedAt());
    }

    @Test
    public void shouldMapEntityToSafeOrderResponse() {
        Ingredient carn = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN);
        Ingredient ched = new Ingredient("CHED", "Cheddar", Ingredient.Type.CHEESE);

        Taco taco = new Taco();
        taco.setName("Taco al pastor");
        taco.setIngredients(Arrays.asList(carn, ched));

        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");
        order.setDeliveryName("Test User");
        order.setDeliveryZip("20000");
        order.setItems(Arrays.asList(line(taco, 2, "2.50", "5.00"))); //modificacion para TC-14
        order.setTotal(new BigDecimal("5.00"));
        order.setCurrency("USD");

        OrderResponse response = orderMapper.toResponse(order);

        assertEquals("ORDER1", response.getId());
        assertEquals("Test User", response.getDeliveryName());
        assertEquals("20000", response.getDeliveryZip());
        assertEquals(1, response.getItems().size()); //modificacion para TC-14
        assertEquals("Taco al pastor", response.getItems().get(0).getTaco().getName());
        assertEquals(2, response.getItems().get(0).getTaco().getIngredients().size());
        assertEquals("CARN", response.getItems().get(0).getTaco().getIngredients().get(0).getId());
        assertEquals(2, response.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("2.50"), response.getItems().get(0).getUnitPriceAtPurchase());
        assertEquals(new BigDecimal("5.00"), response.getTotal());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    public void shouldNotSerializeSensitiveFields() throws Exception {
        User user = new User(
            "testuser",
            "secretPassword",
            "Test User",
            "Test Street",
            "Test City",
            "AG",
            "20000",
            "4491234567",
            "test@gmail.com"
        );

        user.setId("USER1");

        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");
        order.setUser(user);
        order.setDeliveryName("Test User");
        order.setPaymentMethodId("PAYMENT1"); //modificacion para TC-12

        OrderResponse response = orderMapper.toResponse(order);

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("authorities"));
        assertFalse(json.contains("ccNumber"));
        assertFalse(json.contains("ccCVV"));
        assertFalse(json.contains("ccExpiration"));
        assertFalse(json.contains("secretPassword"));
        assertFalse(json.contains("PAYMENT1"));
        assertFalse(json.contains("USER1"));
    }

    //TC-14 - La lectura conserva el precio historico persistido
    @Test
    public void shouldMapHistoricalPriceWithoutRecalculation() {
        Ingredient ingredient = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN,
            new BigDecimal("9.99"), true, 10, 2);
        Taco taco = new Taco();
        taco.setName("Historical taco");
        taco.setIngredients(Collections.singletonList(ingredient));
        TacoOrder order = new TacoOrder();
        order.setItems(Collections.singletonList(line(taco, 2, "1.25", "2.50")));
        order.setTotal(new BigDecimal("2.50"));
        order.setCurrency("USD");

        OrderResponse response = orderMapper.toResponse(order);

        assertEquals(new BigDecimal("1.25"),
            response.getItems().get(0).getUnitPriceAtPurchase());
        assertEquals(new BigDecimal("2.50"), response.getTotal());
    }
    //Fin TC-14

    @Test
    public void shouldIgnoreServerOwnedFields() throws Exception {
        String json =
            "{"
            + "\"id\":\"HACKED\","
            + "\"placedAt\":\"2000-01-01T00:00:00Z\","
            + "\"userId\":\"OTHER_USER\","
            + "\"status\":\"DELIVERED\","
            + "\"total\":0,\"currency\":\"HACK\","
            + "\"deliveryName\":\"Valid User\","
            + "\"deliveryStreet\":\"Test Street\","
            + "\"deliveryCity\":\"Test City\","
            + "\"deliveryState\":\"AG\","
            + "\"deliveryZip\":\"20000\","
            + "\"items\":[{\"taco\":{\"name\":\"Test\","
            + "\"ingredientIds\":[\"CARN\"]},\"quantity\":2,"
            + "\"unitPriceAtPurchase\":0,\"subtotal\":0}]"
            + "}";
    
        ObjectMapper objectMapper = new ObjectMapper();
        OrderCreateRequest request = objectMapper.readValue(json, OrderCreateRequest.class);
    
        TacoOrder order = orderMapper.toEntity(request, Collections.emptyList(),
            new BigDecimal("7.50"), BigDecimal.ZERO, new BigDecimal("7.50"),
            null, "USD"); //modificacion para TC-15
    
        assertNull(order.getId());
        assertNull(order.getUser());
        assertEquals("Valid User",order.getDeliveryName());
        assertEquals("20000", order.getDeliveryZip());
        assertNotNull(order.getPlacedAt());
        assertEquals(new BigDecimal("7.50"), order.getTotal());
        assertEquals("USD", order.getCurrency());
    }
    //fin de pureba

    //TC-14 - Construye una linea persistida para probar el mapper
    private OrderLine line(Taco taco, int quantity, String unitPrice, String subtotal) {
        OrderLine item = new OrderLine();
        item.setTaco(taco);
        item.setQuantity(quantity);
        item.setUnitPriceAtPurchase(new BigDecimal(unitPrice));
        item.setSubtotal(new BigDecimal(subtotal));
        return item;
    }
    //Fin TC-14
}
