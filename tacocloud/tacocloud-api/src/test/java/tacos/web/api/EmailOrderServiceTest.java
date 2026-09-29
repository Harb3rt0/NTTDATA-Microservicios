package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import tacos.Ingredient;
import tacos.TacoOrder;
import tacos.PaymentMethod;
import tacos.Taco;
import tacos.User;
import tacos.api.error.BusinessRuleException;
import tacos.api.error.ApiErrorCodes;
import tacos.api.mapper.IngredientMapper;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.web.api.EmailOrder.EmailTaco;

public class EmailOrderServiceTest {
    //pruebas TC-06
    @Test
    public void shouldConvertEmailOrderWithMultipleTacos() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        PaymentMethodRepository paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        IngredientController ingredientController = Mockito.mock(IngredientController.class);

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo,
            paymentMethodRepo, ingredientController, pricingService(ingredientRepo)); //modificacion para TC-14

        User user = createUser();
        PaymentMethod paymentMethod = createPaymentMethod(user);

        Ingredient carn = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN);
        Ingredient ched = new Ingredient("CHED", "Cheddar", Ingredient.Type.CHEESE);
        Ingredient flto = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);

        EmailOrder.EmailTaco taco1 = new EmailOrder.EmailTaco();
        taco1.setName("Taco al pastor");
        taco1.setIngredients(Arrays.asList("CARN", "CHED"));

        EmailOrder.EmailTaco taco2 = new EmailOrder.EmailTaco();
        taco2.setName("Taco especial");
        taco2.setIngredients(Arrays.asList("FLTO", "CHED"));

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");
        emailOrder.setTacos(Arrays.asList(taco1, taco2));

        when(userRepo.findByEmail("test.taco@gmail.com")).thenReturn(Mono.just(user));
        when(paymentMethodRepo.findByUserId("USER1")).thenReturn(Mono.just(paymentMethod));
        when(ingredientRepo.findById("CARN")).thenReturn(Mono.just(carn));
        when(ingredientRepo.findById("CHED")).thenReturn(Mono.just(ched));
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(flto));

        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .assertNext(order -> {
                assertNotNull(order);
                assertEquals(user, order.getUser());
                assertEquals("PAYMENT1", order.getPaymentMethodId()); //modificacion para TC-12
                assertEquals(2, order.getItems().size()); //modificacion para TC-14
                assertEquals(1, order.getItems().get(0).getQuantity());
                assertEquals("Taco al pastor", order.getItems().get(0).getTaco().getName());
                assertEquals(2, order.getItems().get(0).getTaco().getIngredients().size());
                assertEquals("CARN", order.getItems().get(0).getTaco().getIngredients().get(0).getId());
                assertEquals("CHED", order.getItems().get(0).getTaco().getIngredients().get(1).getId());
                assertEquals("Taco especial", order.getItems().get(1).getTaco().getName());
                assertEquals("FLTO", order.getItems().get(1).getTaco().getIngredients().get(0).getId());
                assertEquals("CHED", order.getItems().get(1).getTaco().getIngredients().get(1).getId());
                assertNotNull(order.getPlacedAt());
            })
            .verifyComplete();

        verify(userRepo).findByEmail("test.taco@gmail.com");
        verify(paymentMethodRepo).findByUserId("USER1");
        verify(ingredientRepo).findById("CARN");
        verify(ingredientRepo, times(2)).findById("CHED");
        verify(ingredientRepo).findById("FLTO");
    }

    @Test
    public void shouldReturnErrorWhenIngredientDoesNotExist() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        PaymentMethodRepository paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        IngredientController ingredientController = Mockito.mock(IngredientController.class);

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo,
            paymentMethodRepo, ingredientController, pricingService(ingredientRepo)); //modificacion para TC-14

        User user = createUser();
        PaymentMethod paymentMethod = createPaymentMethod(user);

        Ingredient carn = new Ingredient("CARN", "Carnitas", Ingredient.Type.PROTEIN);

        EmailOrder.EmailTaco taco = new EmailOrder.EmailTaco();
        taco.setName("Taco incorrecto");
        taco.setIngredients(Arrays.asList("CARN", "INVALID"));

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");
        emailOrder.setTacos(Arrays.asList(taco));

        when(userRepo.findByEmail("test.taco@gmail.com")).thenReturn(Mono.just(user));
        when(paymentMethodRepo.findByUserId("USER1")).thenReturn(Mono.just(paymentMethod));
        when(ingredientRepo.findById("CARN")).thenReturn(Mono.just(carn));
        when(ingredientRepo.findById("INVALID")).thenReturn(Mono.empty());
        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .expectErrorMatches(error ->
                error instanceof BusinessRuleException && ((BusinessRuleException) error).getCode()
                    .equals(ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND)
            )
            .verify();

        verify(ingredientRepo).findById("INVALID");
    }


    @Test
    public void shouldReturnErrorWhenUserDoesNotExist() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        PaymentMethodRepository paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        IngredientController ingredientController = Mockito.mock(IngredientController.class);

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo,
            paymentMethodRepo, ingredientController, pricingService(ingredientRepo)); //modificacion para TC-14

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("unknown@gmail.com");

        when(userRepo.findByEmail("unknown@gmail.com")).thenReturn(Mono.empty());

        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .expectErrorMatches(error ->
                error instanceof BusinessRuleException && ((BusinessRuleException) error).getCode()
                    .equals(ApiErrorCodes.ORDER_USER_NOT_FOUND)
            )
            .verify();

        verify(userRepo).findByEmail("unknown@gmail.com");

        verifyNoInteractions(paymentMethodRepo);
        verifyNoInteractions(ingredientRepo);
    }


    @Test
    public void shouldReturnErrorWhenPaymentMethodDoesNotExist() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        PaymentMethodRepository paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
        IngredientController ingredientController = Mockito.mock(IngredientController.class);

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo,
            paymentMethodRepo, ingredientController, pricingService(ingredientRepo)); //modificacion para TC-14

        User user = createUser();

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");

        when(userRepo.findByEmail("test.taco@gmail.com")).thenReturn(Mono.just(user));

        when(paymentMethodRepo.findByUserId("USER1")).thenReturn(Mono.empty());

        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .expectErrorMatches(error ->
                error instanceof BusinessRuleException && ((BusinessRuleException) error).getCode()
                    .equals(ApiErrorCodes.ORDER_PAYMENT_METHOD_NOT_FOUND)
            )
            .verify();

        verify(userRepo).findByEmail("test.taco@gmail.com");
        verify(paymentMethodRepo).findByUserId("USER1");
        verifyNoInteractions(ingredientRepo);
    }

    private User createUser() {
        User user = new User(
            "testuser",
            "password",
            "Test Taco",
            "Test Street",
            "Test City",
            "AG",
            "20000",
            "4491234567",
            "test.taco@gmail.com"
        );

        user.setId("USER1");

        return user;
    }

    private PaymentMethod createPaymentMethod(User user) {
        PaymentMethod paymentMethod = new PaymentMethod(
            user,
            "labtok_test",
            "LAB_CARD",
            "9999",
            "12/39"
        );

        paymentMethod.setId("PAYMENT1");

        return paymentMethod;
    }

    //TC-14 - Crea la politica de precios usada por el flujo de correo
    private OrderPricingService pricingService(IngredientRepository ingredientRepo) {
        return new OrderPricingService(ingredientRepo,
            new TacoMapper(new IngredientMapper()), 10, "USD");
    }
    //Fin TC-14
}
