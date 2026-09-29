package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Mono;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

//TC-13 - Verifica que los datos seed tengan catalogo coherente
public class Tc13SeedDataTest {

    @Test
    public void shouldCreateValidIngredientSeedData() throws Exception {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        TacoRepository tacoRepo = Mockito.mock(TacoRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(userRepo.save(any(User.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(tacoRepo.save(any(Taco.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(passwordEncoder.encode(any(String.class))).thenReturn("{bcrypt}seed-hash");

        CommandLineRunner runner = new DevelopmentConfig().dataLoader(
            ingredientRepo, userRepo, passwordEncoder, tacoRepo);
        runner.run();

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepo, times(10)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(ingredient -> {
            assertThat(ingredient.getUnitPrice()).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(ingredient.getUnitPrice().scale()).isEqualTo(2);
            assertThat(ingredient.isAvailable()).isTrue();
            assertThat(ingredient.getStockOnHand()).isGreaterThan(0);
            assertThat(ingredient.getReorderLevel()).isGreaterThanOrEqualTo(0);
        });
    }
}
//Fin TC-13
