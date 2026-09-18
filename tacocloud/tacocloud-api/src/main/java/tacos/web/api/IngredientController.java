package tacos.web.api;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import tacos.data.IngredientRepository;

@RestController
@RequestMapping(path="/api/ingredients", produces="application/json")
public class IngredientController {

  private IngredientRepository repo;

  @Autowired
  public IngredientController(IngredientRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public Flux<Ingredient> allIngredients() {
    return repo.findAll();
  }

  @GetMapping("/{id}")
  public Mono<Ingredient> byId(@PathVariable String id) {
    return repo.findById(id);
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
  public Mono<ResponseEntity<Ingredient>> updateIngredient(@PathVariable String id, @RequestBody Ingredient ingredient) {
    if (ingredient.getId() != null && !ingredient.getId().equals(id)) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    ingredient.setId(id);
    return repo.findById(id)
        .flatMap(updatedIngredient -> repo.save(ingredient))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
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
  @PostMapping
  public Mono<ResponseEntity<Ingredient>> postIngredient(@RequestBody @Valid Ingredient ingredient, UriComponentsBuilder ucb) {
    return Mono.just(ingredient)
        .flatMap(repo::save)
        .map(i -> {
          URI location = ucb.path("/api/ingredients/{id}")
            .buildAndExpand(i.getId()).toUri();
          return ResponseEntity.created(location).body(i);
        });
  }
  //TC-03 – Fin

  // @DeleteMapping("/{id}")
  // public void deleteIngredient(@PathVariable String id) {
  //   repo.deleteById(id);
  // }

  //TC-02 - Eliminar de verdad y responder con semantica HTTP
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteIngredient(@PathVariable String id){
    if(id == null || id.isEmpty()){
      return Mono.just(ResponseEntity.badRequest().build());
    }
    return repo.findById(id)
        .flatMap(ingredient -> repo.deleteById(ingredient.getId())
            .then(Mono.just(ResponseEntity.noContent().<Void>build())))
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
  //TC-02 - Fin
}
