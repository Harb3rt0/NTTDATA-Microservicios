package tacos.web.api;

import java.net.URI;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.IngredientResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.IngredientMapper;
import tacos.data.IngredientRepository;

@RestController
@Validated
@RequestMapping(path="/api/ingredients", produces="application/json")
public class IngredientController {

  private final IngredientMapper ingredientMapper;
  private final IngredientCatalogService ingredientCatalogService; //modificacion para TC-13
  private IngredientRepository repo;

  @Autowired
  public IngredientController(IngredientRepository repo, IngredientMapper ingredientMapper,
      IngredientCatalogService ingredientCatalogService) {
    this.repo = repo;
    this.ingredientMapper = ingredientMapper;
    this.ingredientCatalogService = ingredientCatalogService; //modificacion para TC-13
  }

  //TC-08
  @GetMapping
  public Flux<IngredientResponse> allIngredients() {
    return repo.findAll().map(ingredientMapper::toResponse);
  }

  @GetMapping("/{id}")
  public Mono<IngredientResponse> byId(@PathVariable @NotBlank @Size(max = 20) String id) {
    return repo.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.INGREDIENT_NOT_FOUND, "Ingredient was not found.")))
        .map(ingredientMapper::toResponse);
  }

  // @PutMapping("/{id}")
  // public void updateIngredient(@PathVariable String id, @RequestBody Ingredient ingredient) {
  //   if (!ingredient.getId().equals(id)) {
  //     throw new IllegalStateException("Given ingredient's ID doesn't match the ID in the path.");
  //   }
  //   repo.save(ingredient);
  // }

  //TC-01 - Actualizar un ingrediente sin perder el publisher
  @PutMapping("/{id}")
  public Mono<ResponseEntity<IngredientResponse>> updateIngredient(
      @PathVariable @NotBlank @Size(max = 20) String id,
      @Valid @RequestBody IngredientRequest request) {
    if (request.getId() != null && !request.getId().equals(id)) { //modificacion para TC-13
      throw new BadRequestException(ApiErrorCodes.INGREDIENT_ID_MISMATCH,
          "The ingredient id does not match the path id.");
    }
    return ingredientCatalogService.replace(id, request) //modificacion para TC-13
        .map(ingredientMapper::toResponse)
        .map(ResponseEntity::ok);
  }
  //TC-01 - Fin

  // @PostMapping
  // public Mono<ResponseEntity<Ingredient>> postIngredient(@RequestBody Mono<Ingredient> ingredient) {
  //   return ingredient
  //       .flatMap(repo::save)
  //       .map(i -> {
  //         HttpHeaders headers = new HttpHeaders();
  //         headers.setLocation(URI.create("http://localhost:8080/ingredients/" + i.getId()));
  //         return new ResponseEntity<Ingredient>(i, headers, HttpStatus.CREATED);
  //       });
  // }

  //TC-03 - Construir Location sin localhost ni rutas rotas
  //modificacion para TC-08
  @PostMapping
  public Mono<ResponseEntity<IngredientResponse>> postIngredient(@Valid @RequestBody IngredientRequest request, ServerHttpRequest httpRequest) {
    return ingredientCatalogService.create(request).map(saved -> { //modificacion para TC-13
      URI location = UriComponentsBuilder.fromHttpRequest(httpRequest)
        .path("/{id}")
        .buildAndExpand(saved.getId()).toUri();
      
      return ResponseEntity.created(location)
        .body(ingredientMapper.toResponse(saved));
    });
  }
  //TC-03 – Fin

  // @DeleteMapping("/{id}")
  // public void deleteIngredient(@PathVariable String id) {
  //   repo.deleteById(id);
  // }

  //TC-02 - Eliminar de verdad y responder con semantica HTTP
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteIngredient(
      @PathVariable @NotBlank @Size(max = 20) String id){
    return repo.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.INGREDIENT_NOT_FOUND, "Ingredient was not found.")))
        .flatMap(ingredient -> repo.deleteById(ingredient.getId())
            .thenReturn(ResponseEntity.noContent().<Void>build()));
  }
  //TC-02 - Fin
}
