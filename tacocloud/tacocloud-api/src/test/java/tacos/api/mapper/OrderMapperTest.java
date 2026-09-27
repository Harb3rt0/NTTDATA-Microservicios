package tacos.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.Ingredient;
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
        request.setCcNumber("1111222233334444");
        request.setCcExpiration("12/30");
        request.setCcCVV("123");

        Ingredient carn = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN);

        Taco taco = new Taco();
        taco.setName("Taco al pastor");
        taco.setIngredients(Arrays.asList(carn));
        TacoOrder order = orderMapper.toEntity(request, Arrays.asList(taco));

        assertEquals("Test User", order.getDeliveryName());
        assertEquals("Test Street", order.getDeliveryStreet());
        assertEquals("Aguascalientes", order.getDeliveryCity());
        assertEquals("AG", order.getDeliveryState());
        assertEquals("20000", order.getDeliveryZip());
        assertEquals(1, order.getTacos().size());
        assertEquals("Taco al pastor", order.getTacos().get(0).getName());
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
        order.setTacos(Arrays.asList(taco));

        OrderResponse response = orderMapper.toResponse(order);

        assertEquals("ORDER1", response.getId());
        assertEquals("Test User", response.getDeliveryName());
        assertEquals("20000", response.getDeliveryZip());
        assertEquals(1, response.getTacos().size());
        assertEquals("Taco al pastor", response.getTacos().get(0).getName());
        assertEquals(2, response.getTacos().get(0).getIngredients().size());
        assertEquals("CARN", response.getTacos().get(0).getIngredients().get(0).getId());
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
        order.setCcNumber("1111222233334444");
        order.setCcCVV("123");
        order.setCcExpiration("12/30");

        OrderResponse response = orderMapper.toResponse(order);

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("authorities"));
        assertFalse(json.contains("ccNumber"));
        assertFalse(json.contains("ccCVV"));
        assertFalse(json.contains("ccExpiration"));
        assertFalse(json.contains("secretPassword"));
        assertFalse(json.contains("1111222233334444"));
        assertFalse(json.contains("USER1"));
    }

    @Test
    public void shouldIgnoreServerOwnedFields() throws Exception {
        String json =
            "{"
            + "\"id\":\"HACKED\","
            + "\"placedAt\":\"2000-01-01T00:00:00Z\","
            + "\"userId\":\"OTHER_USER\","
            + "\"status\":\"DELIVERED\","
            + "\"total\":0,"
            + "\"deliveryName\":\"Valid User\","
            + "\"deliveryStreet\":\"Test Street\","
            + "\"deliveryCity\":\"Test City\","
            + "\"deliveryState\":\"AG\","
            + "\"deliveryZip\":\"20000\","
            + "\"tacos\":[]"
            + "}";
    
        ObjectMapper objectMapper = new ObjectMapper();
        OrderCreateRequest request = objectMapper.readValue(json, OrderCreateRequest.class);
    
        TacoOrder order = orderMapper.toEntity(request, Collections.emptyList());
    
        assertNull(order.getId());
        assertNull(order.getUser());
        assertEquals("Valid User",order.getDeliveryName());
        assertEquals("20000", order.getDeliveryZip());
        assertNotNull(order.getPlacedAt());
    }
    //fin de pureba
}
