package tacos.web.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.api.dto.TacoOfTheDayResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.TacoMapper;
import tacos.physics.TacoDesignContext;
import tacos.physics.TacoDesignValidator;

//TC-20 - Taco del dia determinista, valido y sin persistencia
@Service
public class TacoOfTheDayService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TacoDesignValidator validator;
    private final TacoMapper tacoMapper;
    private final Clock clock;

    public TacoOfTheDayService(ReactiveMongoTemplate mongoTemplate, TacoDesignValidator validator,
            TacoMapper tacoMapper, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.validator = validator;
        this.tacoMapper = tacoMapper;
        this.clock = clock;
    }

    public Mono<TacoOfTheDayResponse> getToday() {
        Query candidates = new Query(Criteria.where("ingredients").not().elemMatch(
            new Criteria().orOperator(Criteria.where("available").is(false),
                Criteria.where("stockOnHand").lte(0))));
        candidates.with(Sort.by(Sort.Direction.ASC, "_id"));
        return mongoTemplate.find(candidates, Taco.class)
            .filter(taco -> validator.validate(
                new TacoDesignContext(taco.getName(), taco.getIngredients())).isEmpty())
            .sort(Comparator.comparing(Taco::getId))
            .collectList()
            .flatMap(this::select);
    }

    private Mono<TacoOfTheDayResponse> select(List<Taco> candidates) {
        if (candidates.isEmpty()) {
            return Mono.error(new ResourceNotFoundException(ApiErrorCodes.TACO_OF_DAY_NOT_FOUND,
                "No valid and available taco exists for today."));
        }
        LocalDate date = LocalDate.now(clock);
        int index = (int) Math.floorMod(date.toEpochDay(), candidates.size());
        return Mono.just(new TacoOfTheDayResponse(tacoMapper.toResponse(candidates.get(index)), date,
            "Deterministic daily selection among valid tacos with available ingredients."));
    }
}
//Fin TC-20
