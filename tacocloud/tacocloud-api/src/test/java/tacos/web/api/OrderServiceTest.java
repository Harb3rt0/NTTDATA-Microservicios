package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.OrderMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.data.UserRepository;
import tacos.messaging.OrderMessagingService;

public class OrderServiceTest {
    private OrderRepository orderRepo;
    private IngredientRepository ingredientRepo;
    private UserRepository userRepo;
    private OrderMessagingService orderMessages;
    private IngredientMapper ingredientMapper;
    private TacoMapper tacoMapper;
    private OrderMapper orderMapper;
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderRepo = Mockito.mock(OrderRepository.class);
        ingredientRepo = Mockito.mock(IngredientRepository.class);
        userRepo = Mockito.mock(UserRepository.class);
        orderMessages = Mockito.mock(OrderMessagingService.class);
        ingredientMapper = new IngredientMapper();
        tacoMapper = new TacoMapper(ingredientMapper);
        orderMapper = new OrderMapper(tacoMapper);
        
        orderService = new OrderService(orderRepo, ingredientRepo, orderMessages, tacoMapper, orderMapper, userRepo);
    }

    //pruebas TC-07
    @Test
    public void shouldSaveAndPublishExactlyOnce() {
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");

        when(orderRepo.save(order)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.saveAndPublish(order))
            .assertNext(savedOrder -> assertEquals("ORDER1",savedOrder.getId()))
            .verifyComplete();

        verify(orderRepo, times(1)).save(order);
        verify(orderMessages, times(1)).sendOrder(order);
    }

    @Test
    public void shouldNotPublishWhenSaveFails() {
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");

        RuntimeException saveError = new RuntimeException("Database error");

        when(orderRepo.save(order)).thenReturn(Mono.error(saveError));

        StepVerifier.create(orderService.saveAndPublish(order))
            .expectErrorMatches(error -> error instanceof RuntimeException && error.getMessage().equals("Database error"))
            .verify();

        verify(orderRepo, times(1)).save(order);
        verify(orderMessages, never()).sendOrder(order);
    }

    @Test
    public void shouldRejectUnknownIngredientBeforeSaveOrPublish() {
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Test taco");
        taco.setIngredientIds(Collections.singletonList("MISSING"));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setTacos(Collections.singletonList(taco));

        when(ingredientRepo.findById("MISSING")).thenReturn(Mono.empty());
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(user("alice")));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ((BusinessRuleException) error).getCode()
                    .equals(ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND))
            .verify();

        verify(orderRepo, never()).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(TacoOrder.class));
    }
    //final de pruebas TC-07

    //TC-11 - Ownership se valida con la identidad autenticada
    @Test
    public void shouldAssignAuthenticatedUserWhenCreatingOrder() {
        User alice = user("alice");
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Test taco");
        taco.setIngredientIds(Collections.singletonList("FLTO"));
        OrderCreateRequest request = new OrderCreateRequest();
        request.setTacos(Collections.singletonList(taco));

        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .assertNext(order -> assertEquals("alice", order.getUser().getUsername()))
            .verifyComplete();

        verify(userRepo).findByUsername("alice");
        verify(orderRepo, times(1)).save(any(TacoOrder.class));
    }

    @Test
    public void shouldRejectAnotherUsersOrderWithoutSaving() {
        TacoOrder bobsOrder = new TacoOrder();
        bobsOrder.setId("ORDER-B");
        bobsOrder.setUser(user("bob"));
        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setDeliveryZip("12345");
        when(orderRepo.findById("ORDER-B")).thenReturn(Mono.just(bobsOrder));

        StepVerifier.create(orderService.patchOrder(
                "ORDER-B", patch, authentication("alice", "ROLE_USER")))
            .expectError(AccessDeniedException.class)
            .verify();

        verify(orderRepo, never()).save(any(TacoOrder.class));
    }

    @Test
    public void shouldAllowOwnerToModifyOwnOrder() {
        TacoOrder bobsOrder = new TacoOrder();
        bobsOrder.setId("ORDER-B");
        bobsOrder.setUser(user("bob"));
        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setDeliveryZip("12345");
        when(orderRepo.findById("ORDER-B")).thenReturn(Mono.just(bobsOrder));
        when(orderRepo.save(bobsOrder)).thenReturn(Mono.just(bobsOrder));

        StepVerifier.create(orderService.patchOrder(
                "ORDER-B", patch, authentication("bob", "ROLE_USER")))
            .assertNext(order -> assertEquals("12345", order.getDeliveryZip()))
            .verifyComplete();

        verify(orderRepo).save(bobsOrder);
    }

    @Test
    public void shouldQueryOnlyAuthenticatedUsersOrders() {
        when(orderRepo.findByUserUsernameOrderByPlacedAtDesc("alice")).thenReturn(Flux.empty());

        StepVerifier.create(orderService.findOrders(authentication("alice", "ROLE_USER")))
            .verifyComplete();

        verify(orderRepo).findByUserUsernameOrderByPlacedAtDesc("alice");
        verify(orderRepo, never()).findAll();
    }

    private Authentication authentication(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "n/a",
            Collections.singletonList(new SimpleGrantedAuthority(role)));
    }

    private User user(String username) {
        return new User(username, "{bcrypt}hash", "Test User", "Street", "City", "TX",
            "78701", "555-0100", username + "@example.com");
    }
    //Fin TC-11
}
