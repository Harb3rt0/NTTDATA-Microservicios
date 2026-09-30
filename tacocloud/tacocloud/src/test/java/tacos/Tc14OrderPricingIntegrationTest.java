package tacos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.messaging.KitchenOrderEvent;
import tacos.messaging.OrderMessagingService;

//TC-14 - Integracion HTTP de cantidades y precios confiables
@SpringBootTest(properties = {
    "spring.boot.admin.client.enabled=false",
    "management.info.git.enabled=false",
    "management.info.build.enabled=false",
    "tacocloud.messaging.transport=rabbit" //modificacion para TC-36
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class Tc14OrderPricingIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IngredientRepository ingredientRepo;

  @MockBean
  private UserRepository userRepo;

  @MockBean
  private OrderRepository orderRepo;

  @MockBean
  private PaymentMethodRepository paymentMethodRepo;

  @MockBean
  private OrderMessagingService orderMessages;

  @MockBean
  private tacos.web.api.InventoryService inventoryService;

  @MockBean
  private TransactionalOperator transactionalOperator; //modificacion para TC-36

  @BeforeEach
  public void setUp() {
    reset(ingredientRepo, userRepo, orderRepo, paymentMethodRepo, orderMessages, inventoryService,
        transactionalOperator);
    when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0)); //modificacion para TC-36
  }

  @Test
  public void shouldIgnoreClientTotalsAndPersistServerPriceForQuantityTwo() throws Exception {
    User alice = userEntity("alice");
    when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
    when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));
    when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(new Ingredient(
        "FLTO", "Flour Tortilla", Ingredient.Type.WRAP,
        new BigDecimal("1.25"), true, 10, 2)));
    when(ingredientRepo.findById("CARN")).thenReturn(Mono.just(new Ingredient(
        "CARN", "Carnitas", Ingredient.Type.PROTEIN,
        new BigDecimal("2.50"), true, 10, 2)));
    when(inventoryService.reserve(any(), any())).thenReturn(Mono.just(new InventoryReservation()));
    when(inventoryService.confirm(any(), any())).thenReturn(Mono.just(new InventoryReservation()));
    when(orderRepo.save(any(TacoOrder.class))).thenAnswer(invocation -> {
      TacoOrder order = invocation.getArgument(0);
      order.setId("ORDER1");
      return Mono.just(order);
    });

    MvcResult result = mockMvc.perform(post("/api/orders")
            .with(user("alice").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(2, true)))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.items[0].unitPriceAtPurchase").value(3.75))
        .andExpect(jsonPath("$.items[0].subtotal").value(7.50))
        .andExpect(jsonPath("$.total").value(7.50))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.paymentMethodId").doesNotExist())
        .andExpect(jsonPath("$.user").doesNotExist());

    ArgumentCaptor<TacoOrder> orderCaptor = ArgumentCaptor.forClass(TacoOrder.class);
    verify(orderRepo, times(1)).save(orderCaptor.capture());
    assertEquals(new BigDecimal("7.50"), orderCaptor.getValue().getTotal());
    assertEquals(new BigDecimal("3.75"),
        orderCaptor.getValue().getItems().get(0).getUnitPriceAtPurchase());
    verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-29
  }

  @Test
  public void shouldRejectZeroQuantityWithApiProblemBeforePersistence() throws Exception {
    mockMvc.perform(post("/api/orders")
            .with(user("alice").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(0, false)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.violations").isArray());

    verifyNoInteractions(userRepo, paymentMethodRepo, ingredientRepo, orderRepo, orderMessages);
  }

  @Test
  public void shouldRejectQuantityAboveConfiguredMaximumBeforePersistence() throws Exception {
    User alice = userEntity("alice");
    when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
    when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));

    MvcResult result = mockMvc.perform(post("/api/orders")
            .with(user("alice").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(11, false)))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("ITEM_QUANTITY_LIMIT_EXCEEDED"));

    verifyNoInteractions(ingredientRepo);
    verify(orderRepo, never()).save(any(TacoOrder.class));
    verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
  }

  @Test
  public void shouldReadPersistedHistoricalSnapshotWithoutIngredientLookup() throws Exception {
    Taco taco = new Taco();
    taco.setName("Historical taco");
    taco.setIngredients(Collections.singletonList(new Ingredient("FLTO", "Flour Tortilla",
        Ingredient.Type.WRAP, new BigDecimal("9.99"), true, 10, 2)));
    OrderLine item = new OrderLine();
    item.setTaco(taco);
    item.setQuantity(2);
    item.setUnitPriceAtPurchase(new BigDecimal("1.25"));
    item.setSubtotal(new BigDecimal("2.50"));
    TacoOrder order = new TacoOrder();
    order.setId("ORDER1");
    order.setUser(userEntity("alice"));
    order.setItems(Collections.singletonList(item));
    order.setTotal(new BigDecimal("2.50"));
    order.setCurrency("USD");
    when(orderRepo.findByUserUsernameOrderByPlacedAtDesc("alice")).thenReturn(Flux.just(order));

    MvcResult result = mockMvc.perform(get("/api/orders")
            .with(user("alice").roles("USER")))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].items[0].unitPriceAtPurchase").value(1.25))
        .andExpect(jsonPath("$[0].items[0].subtotal").value(2.50))
        .andExpect(jsonPath("$[0].total").value(2.50));

    verifyNoInteractions(ingredientRepo);
  }

  private String orderJson(int quantity, boolean includeFakeTotals) {
    String fakeTotals = includeFakeTotals
        ? ",\"unitPriceAtPurchase\":0,\"subtotal\":0"
        : "";
    return "{\"deliveryName\":\"Lab User\",\"deliveryStreet\":\"Lab Street\","
        + "\"deliveryCity\":\"Lab City\",\"deliveryState\":\"LC\","
        + "\"deliveryZip\":\"12345\",\"paymentMethodId\":\"PAYMENT1\","
        + "\"total\":0,\"currency\":\"MXN\",\"items\":[{\"taco\":{"
        + "\"name\":\"Lab taco\",\"ingredientIds\":[\"FLTO\",\"CARN\"]}," //modificacion para TC-18
        + "\"quantity\":" + quantity + fakeTotals + "}]}";
  }

  private User userEntity(String username) {
    User user = new User(username, "{bcrypt}hash", "Test User", "Street", "City", "TX",
        "78701", "555-0100", username + "@example.com");
    user.setId("USER1");
    return user;
  }

  private PaymentMethod paymentMethod(User user) {
    PaymentMethod paymentMethod = new PaymentMethod(user, "labtok_test", "LAB_CARD",
        "9999", "12/39");
    paymentMethod.setId("PAYMENT1");
    return paymentMethod;
  }
}
//Fin TC-14
