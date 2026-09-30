package tacos.web.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Favorite;
import tacos.api.dto.FavoriteResponse;
import tacos.api.dto.PagedResponse;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.error.ResourceNotFoundException;
import tacos.api.mapper.TacoMapper;
import tacos.data.TacoRepository;
import tacos.data.UserRepository;

//TC-21 - Favoritos reactivos con identidad autenticada e idempotencia
@Service
public class FavoriteService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserRepository userRepo;
    private final TacoRepository tacoRepo;
    private final TacoMapper tacoMapper;
    private final Clock clock;

    public FavoriteService(ReactiveMongoTemplate mongoTemplate, UserRepository userRepo,
            TacoRepository tacoRepo, TacoMapper tacoMapper, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.userRepo = userRepo;
        this.tacoRepo = tacoRepo;
        this.tacoMapper = tacoMapper;
        this.clock = clock;
    }

    public Mono<FavoriteResponse> add(String tacoId, Authentication authentication) {
        return userId(authentication).flatMap(userId -> tacoRepo.findById(tacoId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                ApiErrorCodes.TACO_NOT_FOUND, "Taco was not found.")))
            .flatMap(taco -> {
                Query query = Query.query(Criteria.where("userId").is(userId).and("tacoId").is(tacoId));
                Update update = new Update().setOnInsert("userId", userId).setOnInsert("tacoId", tacoId)
                    .setOnInsert("createdAt", Instant.now(clock));
                return mongoTemplate.findAndModify(query, update,
                    FindAndModifyOptions.options().upsert(true).returnNew(true), Favorite.class)
                    .map(favorite -> new FavoriteResponse(tacoId, favorite.getCreatedAt(),
                        tacoMapper.toResponse(taco)));
            }));
    }

    public Mono<Void> remove(String tacoId, Authentication authentication) {
        return userId(authentication).flatMap(userId -> mongoTemplate.remove(Query.query(
            Criteria.where("userId").is(userId).and("tacoId").is(tacoId)), Favorite.class)).then();
    }

    public Mono<PagedResponse<FavoriteResponse>> list(int page, int size,
            Authentication authentication) {
        if (page < 0 || size < 1 || size > 50) {
            return Mono.error(new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Page must be non-negative and size must be between 1 and 50."));
        }
        return userId(authentication).flatMap(userId -> {
            Query base = Query.query(Criteria.where("userId").is(userId));
            Query paged = Query.of(base).with(Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "_id"))).skip((long) page * size).limit(size);
            Mono<List<FavoriteResponse>> items = mongoTemplate.find(paged, Favorite.class)
                .flatMap(this::toResponse).collectList();
            Mono<Long> count = mongoTemplate.count(base, Favorite.class);
            return Mono.zip(items, count).map(result -> {
                int pages = result.getT2() == 0 ? 0
                    : (int) ((result.getT2() + size - 1) / size);
                return new PagedResponse<>(result.getT1(), page, size, result.getT2(), pages,
                    page + 1 < pages);
            });
        });
    }

    private Mono<FavoriteResponse> toResponse(Favorite favorite) {
        return tacoRepo.findById(favorite.getTacoId())
            .map(taco -> new FavoriteResponse(favorite.getTacoId(), favorite.getCreatedAt(),
                tacoMapper.toResponse(taco)))
            .switchIfEmpty(mongoTemplate.remove(Query.query(Criteria.where("_id").is(favorite.getId())),
                Favorite.class).then(Mono.empty()));
    }

    private Mono<String> userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.error(new AccessDeniedException("Authentication is required."));
        }
        return userRepo.findByUsername(authentication.getName())
            .map(user -> user.getId())
            .switchIfEmpty(Mono.error(new AccessDeniedException("Authenticated user is unavailable.")));
    }
}
//Fin TC-21
