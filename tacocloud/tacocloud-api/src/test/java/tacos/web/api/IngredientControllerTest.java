package tacos.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.data.IngredientRepository;

public class IngredientControllerTest {
    //pruebas TC-01
    @Test
    public void shouldUpdateIngredientWithStepVerifier() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.save(ingredientUpdated)).thenReturn(Mono.just(ingredientUpdated));
        
        IngredientController controller = new IngredientController(ingredientRepo);
        
        Mono<ResponseEntity<Ingredient>> result = controller.updateIngredient("FLTO", ingredientUpdated);
        
        StepVerifier.create(result)
            .expectNextMatches(response -> 
                response.getStatusCode().is2xxSuccessful() && 
                response.getBody().getName().equals("Flour Tortilla Updated"))
            .verifyComplete();
            
        Mockito.verify(ingredientRepo).save(ingredientUpdated);
    }

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
    //final de pruebas TC-01

    //pruebas TC-02
    @Test
    public void shouldDeleteIngredientWithStepVerifier() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.deleteById("FLTO")).thenReturn(Mono.empty());
        
        IngredientController controller = new IngredientController(ingredientRepo);
        
        Mono<ResponseEntity<Void>> result = controller.deleteIngredient("FLTO");
        
        StepVerifier.create(result)
            .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
            .verifyComplete();
            
        Mockito.verify(ingredientRepo).deleteById("FLTO");
    }

    @Test 
    public void shouldReturnNoContentWhenDeletingExistingIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);
        when(ingredientRepo.deleteById(ingredient.getId())).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.delete().uri("/api/ingredients/{id}", ingredient.getId())
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test 
    public void shouldReturnNotFoundWhenDeletingNonExistingIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        String nonExistingId = "LALA";
        Mono<Ingredient> emptyMono = Mono.empty();
        when(ingredientRepo.findById(nonExistingId)).thenReturn(emptyMono);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.delete().uri("/api/ingredients/{id}", nonExistingId)
            .exchange()
            .expectStatus().isNotFound();
    }
    //final de pruebas TC-02

    //pruebas TC-03
    @Test 
    public void shouldInspectLocationHeaderWhenCreatingIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("ONIO", "Onion", Ingredient.Type.VEGGIES);
        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.save(any(Ingredient.class))).thenReturn(ingredientMono);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ingredient)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().valueMatches("Location", ".*/api/ingredients/" + ingredient.getId());
    }

    @Test 
    public void shouldFollowLocationHeaderAndReturn200OnIngredientCreation(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("ONIO", "Onion", Ingredient.Type.VEGGIES);
        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.save(any(Ingredient.class))).thenReturn(ingredientMono);
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        URI location = testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ingredient)
            .exchange()
            .expectStatus().isCreated()
            .returnResult(Ingredient.class).getResponseHeaders().getLocation();

        testClient.get().uri(location)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Ingredient.class)
            .isEqualTo(ingredient);
    }

    @Test 
    public void shouldReturnBadRequestWhenCreatingIngredientWithInvalidData(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient invalidIngredient = new Ingredient("", "", null);

        WebTestClient testClient = WebTestClient.bindToController(
            new IngredientController(ingredientRepo)
        ).build();

        testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidIngredient)
            .exchange()
            .expectStatus().isBadRequest();
    }
    //final de pruebas TC-03
}
