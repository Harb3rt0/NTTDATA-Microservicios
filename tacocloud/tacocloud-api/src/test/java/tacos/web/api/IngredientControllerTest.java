package tacos.web.api;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.data.IngredientRepository;

public class IngredientControllerTest {
    @Test 
    public void shouldUpdateIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        Mono<Ingredient> ingredientUpdatedMono = Mono.just(ingredientUpdated);
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);
        when(ingredientRepo.save(ingredientUpdated)).thenReturn(ingredientUpdatedMono);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredient.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ingredientUpdated)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Ingredient.class)
            .isEqualTo(ingredientUpdated);
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingIngredientWithMismatchedId() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("WRAP", "Flour Tortilla Updated", Ingredient.Type.WRAP);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredient.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ingredientUpdated)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test 
    public void shouldReturnNotFoundWhenUpdatingNonExistingIngredient() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        Mono<Ingredient> emptyMono = Mono.empty();
        when(ingredientRepo.findById(ingredientUpdated.getId())).thenReturn(emptyMono);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredientUpdated.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ingredientUpdated)
            .exchange()
            .expectStatus().isNotFound();
    }
}
