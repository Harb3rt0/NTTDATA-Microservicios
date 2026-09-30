package tacos.web.api;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoRating;
import tacos.api.dto.RatingSummaryResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.TacoMapper;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

//TC-22 - Voto reemplazable y ranking agregado en MongoDB
@Service
public class TacoRatingService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserRepository userRepo;
    private final TacoRepository tacoRepo;
    private final TacoMapper tacoMapper;
    private final Clock clock;
    private final int minVotes;

    public TacoRatingService(ReactiveMongoTemplate mongoTemplate, UserRepository userRepo,
            TacoRepository tacoRepo, TacoMapper tacoMapper, Clock clock,
            @Value("${tacocloud.ratings.min-votes:1}") int minVotes) {
        this.mongoTemplate = mongoTemplate;
        this.userRepo = userRepo;
        this.tacoRepo = tacoRepo;
        this.tacoMapper = tacoMapper;
        this.clock = clock;
        this.minVotes = minVotes;
    }

    public Mono<RatingSummaryResponse> rate(String tacoId, int score,
            Authentication authentication) {
        return userId(authentication).flatMap(userId -> tacoRepo.findById(tacoId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.TACO_NOT_FOUND, "Taco was not found.")))
            .flatMap(taco -> {
                Query query = Query.query(Criteria.where("userId").is(userId).and("tacoId").is(tacoId));
                Update update = new Update().set("score", score).set("updatedAt", Instant.now(clock))
                    .setOnInsert("userId", userId).setOnInsert("tacoId", tacoId);
                return mongoTemplate.findAndModify(query, update,
                    FindAndModifyOptions.options().upsert(true).returnNew(true), TacoRating.class)
                    .then(aggregate(Criteria.where("tacoId").is(tacoId), 1).single())
                    .map(aggregate -> response(tacoMapper.toResponse(taco), aggregate));
            }));
    }

    public Flux<RatingSummaryResponse> top(int limit) {
        if (limit < 1 || limit > 50) {
            return Flux.error(new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Limit must be between 1 and 50."));
        }
        return aggregate(new Criteria(), minVotes).take(limit).collectList().flatMapMany(aggregates -> {
            List<String> tacoIds = aggregates.stream().map(RatingAggregate::getTacoId)
                .collect(Collectors.toList());
            return tacoRepo.findAllById(tacoIds).collectMap(taco -> taco.getId())
                .flatMapMany(tacos -> Flux.fromIterable(aggregates)
                    .filter(item -> tacos.containsKey(item.getTacoId()))
                    .map(item -> response(tacoMapper.toResponse(tacos.get(item.getTacoId())), item)));
        });
    }

    private Flux<RatingAggregate> aggregate(Criteria tacoFilter, int minimumVotes) {
        Aggregation aggregation = newAggregation(
            match(tacoFilter),
            group("tacoId").avg("score").as("average").count().as("votes").push("score").as("scores"),
            match(Criteria.where("votes").gte(minimumVotes)),
            sort(Sort.by(Sort.Order.desc("average"), Sort.Order.desc("votes"), Sort.Order.asc("_id"))));
        return mongoTemplate.aggregate(aggregation, TacoRating.class, RatingAggregate.class);
    }

    private RatingSummaryResponse response(tacos.api.dto.TacoResponse taco, RatingAggregate aggregate) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int score = 1; score <= 5; score++) {
            final int value = score;
            distribution.put(score, aggregate.getScores().stream().filter(item -> item == value).count());
        }
        BigDecimal average = BigDecimal.valueOf(aggregate.getAverage()).setScale(2, RoundingMode.HALF_UP);
        return new RatingSummaryResponse(taco, average, aggregate.getVotes(), distribution);
    }

    private Mono<String> userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.error(new AccessDeniedException("Authentication is required."));
        }
        return userRepo.findByUsername(authentication.getName()).map(user -> user.getId())
            .switchIfEmpty(Mono.error(new AccessDeniedException("Authenticated user is unavailable.")));
    }

    @Data
    private static class RatingAggregate {
        @Id
        private String tacoId;
        private double average;
        private long votes;
        private List<Integer> scores = new ArrayList<>();
    }
}
//Fin TC-22
