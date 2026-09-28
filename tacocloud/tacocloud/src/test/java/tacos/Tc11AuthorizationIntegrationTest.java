package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.data.UserRepository;

@SpringBootTest(properties = {
    "spring.boot.admin.client.enabled=false",
    "tacocloud.security.allowed-origin=http://localhost:4200"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class Tc11AuthorizationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IngredientRepository ingredientRepo;

  @MockBean
  private UserRepository userRepo;

  @MockBean
  private OrderRepository orderRepo;

  @BeforeEach
  public void setUp() {
    reset(ingredientRepo, userRepo, orderRepo);
  }

  //TC-11 - Matriz HTTP deny-by-default y errores ApiProblem
  @Test
  public void shouldKeepIngredientCatalogPublic() throws Exception {
    when(ingredientRepo.findAll()).thenReturn(Flux.empty());

    MvcResult result = mockMvc.perform(get("/api/ingredients"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
  }

  @Test
  public void shouldRejectAnonymousOrderCreationWithApiProblem() throws Exception {
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.instance").value("/api/orders"));
  }

  @Test
  public void shouldAllowUserToListOnlyOrdersSelectedByService() throws Exception {
    when(orderRepo.findByUserUsernameOrderByPlacedAtDesc("alice")).thenReturn(Flux.empty());

    MvcResult result = mockMvc.perform(get("/api/orders").with(user("alice").roles("USER")))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
  }

  @Test
  public void shouldEnforceOrderOwnershipInService() throws Exception {
    User bob = new User("bob", "{bcrypt}hash", "Bob User", "Street", "City", "TX",
        "78701", "555-0100", "bob@example.com");
    TacoOrder order = new TacoOrder();
    order.setId("ORDER-B");
    order.setUser(bob);
    when(orderRepo.findById("ORDER-B")).thenReturn(Mono.just(order));
    when(orderRepo.save(order)).thenReturn(Mono.just(order));

    MvcResult denied = mockMvc.perform(patch("/api/orders/ORDER-B")
            .with(user("alice").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deliveryZip\":\"12345\"}"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(denied))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    verify(orderRepo, never()).save(any(TacoOrder.class));

    MvcResult allowed = mockMvc.perform(patch("/api/orders/ORDER-B")
            .with(user("bob").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deliveryZip\":\"12345\"}"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(allowed))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deliveryZip").value("12345"));
    verify(orderRepo).save(order);
  }

  @Test
  public void shouldRejectUserAndKitchenIngredientAdministrationWithApiProblem() throws Exception {
    String ingredient = "{\"id\":\"FLTO\",\"name\":\"Flour Tortilla\",\"type\":\"WRAP\"}";

    mockMvc.perform(put("/api/ingredients/FLTO").with(user("alice").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(ingredient))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    mockMvc.perform(put("/api/ingredients/FLTO").with(user("cook").roles("KITCHEN"))
            .contentType(MediaType.APPLICATION_JSON).content(ingredient))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  public void shouldAllowAdminIngredientAdministration() throws Exception {
    Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
    when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
    when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(invocation ->
        Mono.just(invocation.<Ingredient>getArgument(0)));

    MvcResult result = mockMvc.perform(put("/api/ingredients/FLTO")
            .with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":\"FLTO\",\"name\":\"Updated Tortilla\",\"type\":\"WRAP\"}"))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Tortilla"));
  }

  @Test
  public void shouldDenyUnlistedRoute() throws Exception {
    mockMvc.perform(get("/api/not-listed").with(user("alice").roles("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  public void shouldProtectDataRestAndAllowAdminPastSecurity() throws Exception {
    mockMvc.perform(get("/data-api/not-listed"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    mockMvc.perform(get("/data-api/not-listed").with(user("alice").roles("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    int adminStatus = mockMvc.perform(get("/data-api/not-listed")
            .with(user("admin").roles("ADMIN")))
        .andReturn().getResponse().getStatus();
    assertThat(adminStatus).isNotIn(401, 403);
  }

  @Test
  public void shouldExposeOnlyHealthPubliclyAndProtectOtherActuatorEndpoints() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

    mockMvc.perform(get("/actuator/info"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    mockMvc.perform(get("/actuator/info").with(user("alice").roles("USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    mockMvc.perform(get("/actuator/info").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  public void shouldAllowOnlyConfiguredCorsOrigin() throws Exception {
    mockMvc.perform(options("/api/orders")
            .header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

    mockMvc.perform(options("/api/orders")
            .header("Origin", "http://malicious.example")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
  //Fin TC-11
}
