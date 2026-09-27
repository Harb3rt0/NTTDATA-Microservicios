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

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo, paymentMethodRepo, ingredientController);

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
                assertEquals("1111222233334444", order.getCcNumber());
                assertEquals("123", order.getCcCVV());
                assertEquals("12/30", order.getCcExpiration());
                assertEquals(2, order.getTacos().size());
                assertEquals("Taco al pastor", order.getTacos().get(0).getName());
                assertEquals(2, order.getTacos().get(0).getIngredients().size());
                assertEquals("CARN", order.getTacos().get(0).getIngredients().get(0).getId());
                assertEquals("CHED", order.getTacos().get(0).getIngredients().get(1).getId());
                assertEquals("Taco especial", order.getTacos().get(1).getName());
                assertEquals("FLTO", order.getTacos().get(1).getIngredients().get(0).getId());
                assertEquals("CHED", order.getTacos().get(1).getIngredients().get(1).getId());
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

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo, paymentMethodRepo, ingredientController);

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
                error instanceof IllegalArgumentException && error.getMessage().contains("Ingredient not found for id: INVALID")
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

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo, paymentMethodRepo, ingredientController);

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("unknown@gmail.com");

        when(userRepo.findByEmail("unknown@gmail.com")).thenReturn(Mono.empty());

        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .expectErrorMatches(error ->
                error instanceof IllegalArgumentException && error.getMessage().contains("User not found for email: unknown@gmail.com")
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

        EmailOrderService emailOrderService = new EmailOrderService(userRepo, ingredientRepo, paymentMethodRepo, ingredientController);

        User user = createUser();

        EmailOrder emailOrder = new EmailOrder();
        emailOrder.setEmail("test.taco@gmail.com");

        when(userRepo.findByEmail("test.taco@gmail.com")).thenReturn(Mono.just(user));

        when(paymentMethodRepo.findByUserId("USER1")).thenReturn(Mono.empty());

        StepVerifier.create(emailOrderService.convertEmailOrderToDomainOrder(Mono.just(emailOrder)))
            .expectErrorMatches(error ->
                error instanceof IllegalArgumentException && error.getMessage().contains("Payment method not found for user: USER1")
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
            "1111222233334444",
            "123",
            "12/30"
        );

        paymentMethod.setId("PAYMENT1");

        return paymentMethod;
    }
}
