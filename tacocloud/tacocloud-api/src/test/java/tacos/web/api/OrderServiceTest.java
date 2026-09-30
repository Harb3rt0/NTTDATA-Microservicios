package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.PaymentMethod;
import tacos.InventoryReservation;
import tacos.ReservationStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.api.dto.OrderCreateRequest;
import tacos.api.dto.OrderLineCreateRequest;
import tacos.api.dto.OrderPatchRequest;
import tacos.api.dto.OrderQuoteRequest;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.KitchenOrderEventMapper;
import tacos.api.mapper.OrderMapper;
import tacos.api.mapper.TacoMapper;
import tacos.coupon.CouponProperties;
import tacos.coupon.CouponService;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.messaging.KitchenOrderEvent;
import tacos.messaging.OrderMessagingService;
import tacos.physics.AvailabilityRule;
import tacos.physics.BaseCountRule;
import tacos.physics.DuplicateIngredientRule;
import tacos.physics.HotRequiresTypeRule;
import tacos.physics.IngredientCountRule;
import tacos.physics.TacoDesignValidator;
import tacos.physics.TacoDesignException;
import tacos.physics.TypePairRule;

public class OrderServiceTest {
    private OrderRepository orderRepo;
    private IngredientRepository ingredientRepo;
    private UserRepository userRepo;
    private PaymentMethodRepository paymentMethodRepo;
    private OrderMessagingService orderMessages;
    private IngredientMapper ingredientMapper;
    private TacoMapper tacoMapper;
    private OrderMapper orderMapper;
    private KitchenOrderEventMapper kitchenOrderEventMapper;
    private OrderPricingService pricingService;
    private OrderService orderService;
    private InventoryService inventoryService;

    @BeforeEach
    public void setUp() {
        orderRepo = Mockito.mock(OrderRepository.class);
        ingredientRepo = Mockito.mock(IngredientRepository.class);
        userRepo = Mockito.mock(UserRepository.class);
        paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        orderMessages = Mockito.mock(OrderMessagingService.class);
        ingredientMapper = new IngredientMapper();
        tacoMapper = new TacoMapper(ingredientMapper);
        orderMapper = new OrderMapper(tacoMapper);
        kitchenOrderEventMapper = new KitchenOrderEventMapper();
        pricingService = new OrderPricingService(ingredientRepo, tacoMapper, 10, "USD");
        inventoryService = Mockito.mock(InventoryService.class);
        CouponService couponService = new CouponService(new CouponProperties(),
            Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC));
        TacoDesignValidator validator = new TacoDesignValidator(Arrays.asList(
            new BaseCountRule(), new IngredientCountRule(), new DuplicateIngredientRule(),
            new AvailabilityRule(), new HotRequiresTypeRule(Ingredient.Type.VEGGIES),
            new TypePairRule(Ingredient.Type.CHEESE, Ingredient.Type.SAUCE)));
        TacoDesignService tacoDesignService = new TacoDesignService(ingredientRepo, tacoMapper,
            validator, new TacoClassificationService(), pricingService);
        when(inventoryService.reserve(any(String.class), any(java.util.List.class)))
            .thenAnswer(invocation -> {
                InventoryReservation reservation = new InventoryReservation();
                reservation.setId(invocation.getArgument(0));
                reservation.setReservationKey(invocation.getArgument(0));
                reservation.setStatus(ReservationStatus.ACTIVE);
                return Mono.just(reservation);
            });
        when(inventoryService.confirm(any(String.class), any()))
            .thenAnswer(invocation -> Mono.just(new InventoryReservation()));
        when(inventoryService.release(Mockito.nullable(String.class))).thenReturn(Mono.empty());
        
        orderService = new OrderService(orderRepo, orderMessages, orderMapper, userRepo,
            paymentMethodRepo, kitchenOrderEventMapper, pricingService,
            couponService, tacoDesignService, inventoryService); //modificacion para TC-16
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
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-29
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
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-12
    }

    //TC-14 - La orden no se emite antes de terminar la persistencia
    @Test
    public void shouldEmitOrderOnlyAfterSaveCompletes() {
        TacoOrder order = new TacoOrder();
        order.setId("ORDER1");
        MonoProcessor<TacoOrder> saveResult = MonoProcessor.create();
        when(orderRepo.save(order)).thenReturn(saveResult);

        StepVerifier.create(orderService.saveAndPublish(order))
            .expectSubscription()
            .then(() -> {
                verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
                saveResult.onNext(order);
            })
            .expectNext(order)
            .verifyComplete();

        verify(orderRepo, times(1)).save(order);
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-29
    }
    //Fin TC-14

    @Test
    public void shouldRejectUnknownIngredientBeforeSaveOrPublish() {
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Test taco");
        taco.setIngredientIds(Collections.singletonList("MISSING"));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("PAYMENT1"); //modificacion para TC-12
        request.setItems(Collections.singletonList(item(taco, 1))); //modificacion para TC-14

        when(ingredientRepo.findById("MISSING")).thenReturn(Mono.empty());
        User alice = user("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ((BusinessRuleException) error).getCode()
                    .equals(ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND))
            .verify();

        verify(orderRepo, never()).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-12
        verify(inventoryService, never()).reserve(any(String.class), any(java.util.List.class));
    }
    //final de pruebas TC-07

    //TC-11 - Ownership se valida con la identidad autenticada
    @Test
    public void shouldAssignAuthenticatedUserWhenCreatingOrder() {
        User alice = user("alice");
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP,
            new BigDecimal("1.25"), true, 10, 2); //modificacion para TC-14
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Test taco");
        taco.setIngredientIds(Arrays.asList("FLTO", "LETC")); //modificacion para TC-18
        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("PAYMENT1"); //modificacion para TC-12
        request.setItems(Collections.singletonList(item(taco, 2))); //modificacion para TC-14

        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.findById("LETC")).thenReturn(Mono.just(new Ingredient(
            "LETC", "Lettuce", Ingredient.Type.VEGGIES, new BigDecimal("0.65"), true, 10, 2)));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .assertNext(order -> {
                assertEquals("alice", order.getUser().getUsername());
                assertEquals(new BigDecimal("3.80"), order.getTotal());
                assertEquals(new BigDecimal("3.80"), order.getItems().get(0).getSubtotal());
            })
            .verifyComplete();

        verify(userRepo).findByUsername("alice");
        verify(orderRepo, times(1)).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class)); //modificacion para TC-29
    }

    //TC-15 - Quote calcula sin persistir ni publicar
    @Test
    public void shouldQuoteWithoutSavingOrder() {
        User alice = user("alice");
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP,
            new BigDecimal("1.25"), true, 10, 2);
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Quote taco");
        taco.setIngredientIds(Arrays.asList("FLTO", "LETC")); //modificacion para TC-18
        OrderQuoteRequest request = new OrderQuoteRequest();
        request.setItems(Collections.singletonList(item(taco, 2)));
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.findById("LETC")).thenReturn(Mono.just(new Ingredient(
            "LETC", "Lettuce", Ingredient.Type.VEGGIES, new BigDecimal("0.65"), true, 10, 2)));

        StepVerifier.create(orderService.quoteOrder(request, authentication("alice", "ROLE_USER")))
            .assertNext(quote -> {
                assertEquals(new BigDecimal("3.80"), quote.getSubtotalBeforeDiscount());
                assertEquals(new BigDecimal("0.00"), quote.getDiscountAmount());
                assertEquals(new BigDecimal("3.80"), quote.getTotal());
            })
            .verifyComplete();

        verify(orderRepo, never()).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
        verify(paymentMethodRepo, never()).findById(any(String.class));
        verify(inventoryService, never()).reserve(any(String.class), any(java.util.List.class));
    }
    //Fin TC-15

    //TC-18 - Quote y create usan las mismas reglas antes de reservar
    @Test
    public void shouldUseSameRulesForQuoteAndCreateBeforeInventoryReservation() {
        User alice = user("alice");
        Ingredient flour = new Ingredient("FLTO", "Flour", Ingredient.Type.WRAP,
            BigDecimal.ONE, true, 10, 2);
        Ingredient corn = new Ingredient("COTO", "Corn", Ingredient.Type.WRAP,
            BigDecimal.ONE, true, 10, 2);
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Invalid bases");
        taco.setIngredientIds(Arrays.asList("FLTO", "COTO"));
        OrderQuoteRequest quote = new OrderQuoteRequest();
        quote.setItems(Collections.singletonList(item(taco, 1)));
        OrderCreateRequest create = new OrderCreateRequest();
        create.setPaymentMethodId("PAYMENT1");
        create.setItems(Collections.singletonList(item(taco, 1)));
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(flour));
        when(ingredientRepo.findById("COTO")).thenReturn(Mono.just(corn));

        StepVerifier.create(orderService.quoteOrder(quote, authentication("alice", "ROLE_USER")))
            .expectErrorMatches(error -> error instanceof TacoDesignException
                && ((TacoDesignException) error).getViolations().stream()
                    .anyMatch(v -> "INVALID_BASE_COUNT".equals(v.getCode())))
            .verify();
        StepVerifier.create(orderService.createOrder(create, authentication("alice", "ROLE_USER")))
            .expectErrorMatches(error -> error instanceof TacoDesignException
                && ((TacoDesignException) error).getViolations().stream()
                    .anyMatch(v -> "INVALID_BASE_COUNT".equals(v.getCode())))
            .verify();

        verify(inventoryService, never()).reserve(any(String.class), any(java.util.List.class));
        verify(orderRepo, never()).save(any(TacoOrder.class));
    }
    //Fin TC-18

    //TC-16 - Un fallo de persistencia libera la reserva exactamente una vez
    @Test
    public void shouldReleaseReservationWhenOrderFailsBeforePersistence() {
        User alice = user("alice");
        Ingredient flour = new Ingredient("FLTO", "Flour", Ingredient.Type.WRAP,
            BigDecimal.ONE, true, 10, 2);
        Ingredient lettuce = new Ingredient("LETC", "Lettuce", Ingredient.Type.VEGGIES,
            BigDecimal.ONE, true, 10, 2);
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Valid taco");
        taco.setIngredientIds(Arrays.asList("FLTO", "LETC"));
        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("PAYMENT1");
        request.setItems(Collections.singletonList(item(taco, 1)));
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(flour));
        when(ingredientRepo.findById("LETC")).thenReturn(Mono.just(lettuce));
        when(orderRepo.save(any(TacoOrder.class))).thenReturn(Mono.error(new RuntimeException("save failed")));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .expectErrorMessage("save failed")
            .verify();

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(inventoryService, times(1)).reserve(key.capture(), any(java.util.List.class));
        verify(inventoryService, times(1)).release(key.getValue());
        verify(inventoryService, never()).confirm(any(String.class), any());
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
    }
    //Fin TC-16

    //TC-14 - Conserva snapshots despues de cambiar el precio del catalogo
    @Test
    public void shouldKeepHistoricalPriceAfterCatalogPriceChanges() {
        User alice = user("alice");
        Ingredient first = new Ingredient("A", "Ingredient A", Ingredient.Type.WRAP,
            new BigDecimal("1.25"), true, 10, 2);
        Ingredient second = new Ingredient("B", "Ingredient B", Ingredient.Type.PROTEIN,
            new BigDecimal("2.30"), true, 10, 2);
        TacoCreateRequest taco = new TacoCreateRequest();
        taco.setName("Historical taco");
        taco.setIngredientIds(Arrays.asList("A", "B"));
        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("PAYMENT1");
        request.setItems(Collections.singletonList(item(taco, 2)));
        AtomicReference<TacoOrder> persisted = new AtomicReference<>();

        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(alice));
        when(paymentMethodRepo.findById("PAYMENT1")).thenReturn(Mono.just(paymentMethod(alice)));
        when(ingredientRepo.findById("A")).thenReturn(Mono.just(first));
        when(ingredientRepo.findById("B")).thenReturn(Mono.just(second));
        when(orderRepo.save(any(TacoOrder.class))).thenAnswer(invocation -> {
            TacoOrder order = invocation.getArgument(0);
            order.setId("ORDER1");
            persisted.set(order);
            return Mono.just(order);
        });

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .assertNext(order -> {
                assertEquals(new BigDecimal("3.55"),
                    order.getItems().get(0).getUnitPriceAtPurchase());
                assertEquals(new BigDecimal("7.10"), order.getItems().get(0).getSubtotal());
                assertEquals(new BigDecimal("7.10"), order.getTotal());
            })
            .verifyComplete();

        second.setUnitPrice(new BigDecimal("5.00"));
        when(orderRepo.findByUserUsernameOrderByPlacedAtDesc("alice"))
            .thenReturn(Flux.just(persisted.get()));

        StepVerifier.create(orderService.findOrders(authentication("alice", "ROLE_USER")))
            .assertNext(order -> {
                assertEquals(new BigDecimal("3.55"),
                    order.getItems().get(0).getUnitPriceAtPurchase());
                assertEquals(new BigDecimal("7.10"), order.getItems().get(0).getSubtotal());
                assertEquals(new BigDecimal("7.10"), order.getTotal());
            })
            .verifyComplete();

        verify(ingredientRepo, times(1)).findById("A");
        verify(ingredientRepo, times(1)).findById("B");
    }
    //Fin TC-14

    //TC-12 - Un pago inexistente o ajeno detiene persistencia y evento
    @Test
    public void shouldRejectMissingPaymentMethodBeforeSaveOrPublish() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("MISSING");
        request.setItems(Collections.emptyList()); //modificacion para TC-14
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(user("alice")));
        when(paymentMethodRepo.findById("MISSING")).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .expectErrorMatches(error -> error instanceof ResourceNotFoundException
                && ((ResourceNotFoundException) error).getCode()
                    .equals(ApiErrorCodes.PAYMENT_METHOD_NOT_FOUND))
            .verify();

        verify(orderRepo, never()).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
    }

    @Test
    public void shouldRejectAnotherUsersPaymentMethodBeforeSaveOrPublish() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setPaymentMethodId("PAYMENT-BOB");
        request.setItems(Collections.emptyList()); //modificacion para TC-14
        when(userRepo.findByUsername("alice")).thenReturn(Mono.just(user("alice")));
        when(paymentMethodRepo.findById("PAYMENT-BOB"))
            .thenReturn(Mono.just(paymentMethod(user("bob"))));

        StepVerifier.create(orderService.createOrder(request, authentication("alice", "ROLE_USER")))
            .expectError(AccessDeniedException.class)
            .verify();

        verify(orderRepo, never()).save(any(TacoOrder.class));
        verify(orderMessages, never()).sendOrder(any(KitchenOrderEvent.class));
    }
    //Fin TC-12

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

    private PaymentMethod paymentMethod(User user) {
        PaymentMethod paymentMethod = new PaymentMethod(user, "labtok_test", "LAB_CARD",
            "9999", "12/39");
        paymentMethod.setId("PAYMENT1");
        return paymentMethod;
    }

    //TC-14 - Construye una linea de entrada para las pruebas de orden
    private OrderLineCreateRequest item(TacoCreateRequest taco, int quantity) {
        OrderLineCreateRequest item = new OrderLineCreateRequest();
        item.setTaco(taco);
        item.setQuantity(quantity);
        return item;
    }
    //Fin TC-14
    //Fin TC-11
}
