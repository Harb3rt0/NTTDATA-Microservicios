package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.api.dto.TacoClassificationResponse;
import tacos.api.dto.PagedResponse;
import tacos.api.dto.TacoResponse;
import tacos.api.dto.TacoSearchCriteria;
import tacos.api.dto.TacoOfTheDayResponse;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ResourceNotFoundException;
import tacos.data.TacoRepository;

@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
public class TacoController {
  private TacoRepository tacoRepo;
  private final TacoClassificationService classificationService;
  private final TacoCatalogService catalogService;
  private final TacoOfTheDayService tacoOfTheDayService;

  @Autowired
  public TacoController(TacoRepository tacoRepo,
      TacoClassificationService classificationService, TacoCatalogService catalogService,
      TacoOfTheDayService tacoOfTheDayService) { //modificacion para TC-20
    this.tacoRepo = tacoRepo;
    this.classificationService = classificationService;
    this.catalogService = catalogService;
    this.tacoOfTheDayService = tacoOfTheDayService;
  }

  @GetMapping(params="recent")
  public Flux<Taco> recentTacos() {
    return tacoRepo.findAll().take(12);
  }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Taco> postTaco(@Valid @RequestBody Taco taco) {
    return tacoRepo.save(taco);
  }

  @GetMapping("/{id}")
  public Mono<Taco> tacoById(@PathVariable("id") String id) {
    return tacoRepo.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.TACO_NOT_FOUND, "Taco was not found.")));
  }

  public TacoController(TacoRepository tacoRepo) {
    this.tacoRepo = tacoRepo;
    this.classificationService = new TacoClassificationService();
    this.catalogService = null;
    this.tacoOfTheDayService = null;
  }

  //TC-20 - Recomendacion publica del dia
  @GetMapping("/today")
  public Mono<TacoOfTheDayResponse> today() {
    return tacoOfTheDayService.getToday();
  }
  //Fin TC-20

  //TC-19 - Catalogo filtrable y paginado
  @GetMapping
  public Mono<PagedResponse<TacoResponse>> catalog(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String ingredientId,
      @RequestParam(required = false) DietaryTag diet,
      @RequestParam(required = false) Allergen excludeAllergen,
      @RequestParam(required = false) SpiceLevel spice,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    TacoSearchCriteria search = new TacoSearchCriteria();
    search.setName(name);
    search.setIngredientId(ingredientId);
    search.setDiet(diet);
    search.setExcludeAllergen(excludeAllergen);
    search.setSpice(spice);
    search.setPage(page);
    search.setSize(size);
    search.setSort(sort);
    search.setDirection(direction);
    return catalogService.search(search);
  }
  //Fin TC-19

  //TC-17 - Expone clasificacion derivada por el servidor
  @GetMapping("/{id}/classification")
  public Mono<TacoClassificationResponse> classification(@PathVariable("id") String id) {
    return tacoRepo.findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
            ApiErrorCodes.TACO_NOT_FOUND, "Taco was not found.")))
        .map(taco -> classificationService.classify(taco.getIngredients()));
  }
  //Fin TC-17

}
