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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.IngredientResponse;
import tacos.api.mapper.IngredientMapper;
import tacos.data.IngredientRepository;

public class IngredientControllerTest {
    //TC-13 - El catalogo publico omite metadata operativa
    @Test
    public void shouldExposePublicCatalogWithoutOperationalMetadata() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        IngredientMapper ingredientMapper = new IngredientMapper();
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP,
            new java.math.BigDecimal("1.25"), true, 10, 2);
        ingredient.setVersion(3L);
        when(ingredientRepo.findAll()).thenReturn(Flux.just(ingredient));

        WebTestClient.bindToController(controller(ingredientRepo, ingredientMapper)).build()
            .get().uri("/api/ingredients")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].id").isEqualTo("FLTO")
            .jsonPath("$[0].unitPrice").isEqualTo(1.25)
            .jsonPath("$[0].available").isEqualTo(true)
            .jsonPath("$[0].stockOnHand").doesNotExist()
            .jsonPath("$[0].reorderLevel").doesNotExist()
            .jsonPath("$[0].version").doesNotExist();
    }
    //Fin TC-13

    //pruebas TC-01
    @Test
    public void shouldUpdateIngredientWithStepVerifier() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        IngredientMapper ingredientMapper = new IngredientMapper();
        
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.save(any(Ingredient.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); //modificacion para TC-13
        
        IngredientController controller = controller(ingredientRepo, ingredientMapper);
        
        Mono<ResponseEntity<IngredientResponse>> result = controller.updateIngredient(
            "FLTO", request(ingredientUpdated)); //modificacion para TC-13
        
        StepVerifier.create(result)
            .expectNextMatches(response -> 
                response.getStatusCode().is2xxSuccessful() && 
                response.getBody().getName().equals("Flour Tortilla Updated"))
            .verifyComplete();
            
        Mockito.verify(ingredientRepo).save(any(Ingredient.class));
    }

    @Test 
    public void shouldUpdateIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        IngredientMapper ingredientMapper = new IngredientMapper();

        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);
        when(ingredientRepo.save(any(Ingredient.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); //modificacion para TC-13

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredient.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(ingredientUpdated)) //modificacion para TC-13
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("Flour Tortilla Updated");
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingIngredientWithMismatchedId() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        Ingredient ingredientUpdated = new Ingredient("WRAP", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        IngredientMapper ingredientMapper = new IngredientMapper();

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredient.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(ingredientUpdated)) //modificacion para TC-13
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test 
    public void shouldReturnNotFoundWhenUpdatingNonExistingIngredient() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredientUpdated = new Ingredient("FLTO", "Flour Tortilla Updated", Ingredient.Type.WRAP);
        IngredientMapper ingredientMapper = new IngredientMapper();

        Mono<Ingredient> emptyMono = Mono.empty();
        when(ingredientRepo.findById(ingredientUpdated.getId())).thenReturn(emptyMono);

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.put().uri("/api/ingredients/{id}", ingredientUpdated.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(ingredientUpdated)) //modificacion para TC-13
            .exchange()
            .expectStatus().isNotFound();
    }
    //final de pruebas TC-01

    //pruebas TC-02
    @Test
    public void shouldDeleteIngredientWithStepVerifier() {
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Ingredient.Type.WRAP);
        IngredientMapper ingredientMapper = new IngredientMapper();
        
        when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
        when(ingredientRepo.deleteById("FLTO")).thenReturn(Mono.empty());
        
        IngredientController controller = controller(ingredientRepo, ingredientMapper);
        
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
        IngredientMapper ingredientMapper = new IngredientMapper();

        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);
        when(ingredientRepo.deleteById(ingredient.getId())).thenReturn(Mono.empty());

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.delete().uri("/api/ingredients/{id}", ingredient.getId())
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test 
    public void shouldReturnNotFoundWhenDeletingNonExistingIngredient(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        IngredientMapper ingredientMapper = new IngredientMapper();

        String nonExistingId = "LALA";
        Mono<Ingredient> emptyMono = Mono.empty();
        when(ingredientRepo.findById(nonExistingId)).thenReturn(emptyMono);

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
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
        IngredientMapper ingredientMapper = new IngredientMapper();

        Ingredient ingredient = new Ingredient("ONIO", "Onion", Ingredient.Type.VEGGIES);
        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0))); //modificacion para TC-13

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(ingredient)) //modificacion para TC-13
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().valueMatches("Location", ".*/api/ingredients/" + ingredient.getId());
    }

    @Test 
    public void shouldFollowLocationHeaderAndReturn200OnIngredientCreation(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient ingredient = new Ingredient("ONIO", "Onion", Ingredient.Type.VEGGIES);
        IngredientMapper ingredientMapper = new IngredientMapper();

        Mono<Ingredient> ingredientMono = Mono.just(ingredient);
        when(ingredientRepo.save(any(Ingredient.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0))); //modificacion para TC-13
        when(ingredientRepo.findById(ingredient.getId())).thenReturn(ingredientMono);

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        URI location = testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(ingredient)) //modificacion para TC-13
            .exchange()
            .expectStatus().isCreated()
            .returnResult(IngredientResponse.class).getResponseHeaders().getLocation();

        testClient.get().uri(location)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("ONIO")
            .jsonPath("$.stockOnHand").doesNotExist(); //modificacion para TC-13
    }

    @Test 
    public void shouldReturnBadRequestWhenCreatingIngredientWithInvalidData(){
        IngredientRepository ingredientRepo = Mockito.mock(IngredientRepository.class);
        Ingredient invalidIngredient = new Ingredient("", "", null);
        IngredientMapper ingredientMapper = new IngredientMapper();

        WebTestClient testClient = WebTestClient.bindToController(
            controller(ingredientRepo, ingredientMapper)
        ).build();

        testClient.post().uri("/api/ingredients")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request(invalidIngredient)) //modificacion para TC-13
            .exchange()
            .expectStatus().isBadRequest();
    }
    //final de pruebas TC-03

    //TC-13 - Construye dependencias reales para pruebas aisladas
    private IngredientController controller(IngredientRepository ingredientRepo,
            IngredientMapper ingredientMapper) {
        return new IngredientController(ingredientRepo, ingredientMapper,
            new IngredientCatalogService(ingredientRepo, ingredientMapper));
    }

    private IngredientRequest request(Ingredient ingredient) {
        IngredientRequest request = new IngredientRequest();
        request.setId(ingredient.getId());
        request.setName(ingredient.getName());
        request.setType(ingredient.getType());
        request.setUnitPrice(ingredient.getUnitPrice());
        request.setAvailable(ingredient.isAvailable());
        request.setStockOnHand(ingredient.getStockOnHand());
        request.setReorderLevel(ingredient.getReorderLevel());
        return request;
    }
    //Fin TC-13
}
