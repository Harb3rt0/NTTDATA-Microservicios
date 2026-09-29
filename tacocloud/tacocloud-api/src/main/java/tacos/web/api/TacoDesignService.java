package tacos.web.api;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.OrderLine;
import tacos.Taco;
import tacos.api.dto.OrderLineCreateRequest;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.dto.TacoValidationResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;
import tacos.api.mapper.TacoMapper;
import tacos.data.IngredientRepository;
import tacos.physics.RuleViolation;
import tacos.physics.TacoDesignContext;
import tacos.physics.TacoDesignValidator;

//TC-18 - Resuelve, valida y reutiliza el mismo contexto de diseno
@Service
public class TacoDesignService {
    private final IngredientRepository ingredientRepo;
    private final TacoMapper tacoMapper;
    private final TacoDesignValidator validator;
    private final TacoClassificationService classificationService;
    private final OrderPricingService pricingService;

    public TacoDesignService(IngredientRepository ingredientRepo, TacoMapper tacoMapper,
            TacoDesignValidator validator, TacoClassificationService classificationService,
            OrderPricingService pricingService) {
        this.ingredientRepo = ingredientRepo;
        this.tacoMapper = tacoMapper;
        this.validator = validator;
        this.classificationService = classificationService;
        this.pricingService = pricingService;
    }

    public Mono<TacoDesignContext> resolve(TacoCreateRequest request) {
        List<String> ids = request.getIngredientIds();
        return Flux.fromIterable(new LinkedHashSet<>(ids))
            .concatMap(id -> ingredientRepo.findById(id)
                .switchIfEmpty(Mono.error(new BusinessRuleException(
                    ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND,
                    "Ingredient '" + id + "' is not available."))))
            .collectMap(Ingredient::getId, ingredient -> ingredient, LinkedHashMap::new)
            .map(byId -> new TacoDesignContext(request.getName(), ids.stream()
                .map(byId::get).collect(Collectors.toList())));
    }

    public Mono<OrderLine> resolveValidateAndPrice(OrderLineCreateRequest request) {
        return resolve(request.getTaco()).map(context -> {
            validator.requireValid(context);
            Taco taco = tacoMapper.toEntity(request.getTaco(), context.getIngredients());
            return pricingService.priceResolvedTaco(taco, request.getQuantity());
        });
    }

    public Flux<OrderLine> resolveValidateAndPrice(List<OrderLineCreateRequest> requests) {
        return Flux.fromIterable(requests).concatMap(this::resolveValidateAndPrice);
    }

    public Mono<TacoValidationResponse> validate(TacoCreateRequest request) {
        return resolve(request).map(context -> {
            List<RuleViolation> violations = validator.validate(context);
            TacoValidationResponse response = new TacoValidationResponse();
            response.setValid(violations.isEmpty());
            response.setViolations(violations);
            response.setClassification(classificationService.classify(context.getIngredients()));
            return response;
        });
    }
}
//Fin TC-18
