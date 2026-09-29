package tacos.data;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.OptimisticLockingFailureException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;

//TC-13 - Verifica optimistic locking real con Mongo embebido
@DataMongoTest
public class IngredientOptimisticLockingTest {

    @Autowired
    private IngredientRepository ingredientRepo;

    @Test
    public void shouldRejectStaleIngredientVersion() {
        Ingredient ingredient = new Ingredient("TC13-LOCK", "Lock Test", Ingredient.Type.WRAP,
            new BigDecimal("1.25"), true, 10, 2);

        Mono<Void> concurrentUpdate = ingredientRepo.deleteById(ingredient.getId())
            .then(ingredientRepo.save(ingredient))
            .flatMap(saved -> Mono.zip(
                ingredientRepo.findById(saved.getId()),
                ingredientRepo.findById(saved.getId())))
            .flatMap(copies -> {
                Ingredient firstCopy = copies.getT1();
                Ingredient staleCopy = copies.getT2();
                firstCopy.setName("First update");
                staleCopy.setName("Stale update");
                return ingredientRepo.save(firstCopy)
                    .then(ingredientRepo.save(staleCopy));
            })
            .then();

        StepVerifier.create(concurrentUpdate)
            .expectError(OptimisticLockingFailureException.class)
            .verify();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
//Fin TC-13
