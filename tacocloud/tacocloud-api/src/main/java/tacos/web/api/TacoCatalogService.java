package tacos.web.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.SpiceLevel;
import tacos.Taco;
import tacos.api.dto.PagedResponse;
import tacos.api.dto.TacoResponse;
import tacos.api.dto.TacoSearchCriteria;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BadRequestException;
import tacos.api.mapper.TacoMapper;

//TC-19 - Busqueda, filtros, orden y paginacion ejecutados en MongoDB
@Service
public class TacoCatalogService {
    private static final List<String> ALLOWED_SORTS = Arrays.asList("createdAt", "name");
    private final ReactiveMongoTemplate mongoTemplate;
    private final TacoMapper tacoMapper;

    public TacoCatalogService(ReactiveMongoTemplate mongoTemplate, TacoMapper tacoMapper) {
        this.mongoTemplate = mongoTemplate;
        this.tacoMapper = tacoMapper;
    }

    public Mono<PagedResponse<TacoResponse>> search(TacoSearchCriteria search) {
        validate(search);
        Query query = new Query(buildCriteria(search));
        Sort.Direction direction = "asc".equalsIgnoreCase(search.getDirection())
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, search.getSort()).and(Sort.by(Sort.Direction.ASC, "_id")));

        Query pageQuery = Query.of(query).skip((long) search.getPage() * search.getSize())
            .limit(search.getSize());
        Mono<List<TacoResponse>> items = mongoTemplate.find(pageQuery, Taco.class)
            .map(tacoMapper::toResponse).collectList();
        Mono<Long> total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Taco.class);

        return Mono.zip(items, total).map(result -> {
            long count = result.getT2();
            int pages = count == 0 ? 0 : (int) ((count + search.getSize() - 1) / search.getSize());
            return new PagedResponse<>(result.getT1(), search.getPage(), search.getSize(), count,
                pages, search.getPage() + 1 < pages);
        });
    }

    private Criteria buildCriteria(TacoSearchCriteria search) {
        List<Criteria> filters = new ArrayList<>();
        if (hasText(search.getName())) {
            filters.add(Criteria.where("name").regex(Pattern.compile(
                Pattern.quote(search.getName().trim()), Pattern.CASE_INSENSITIVE)));
        }
        if (hasText(search.getIngredientId())) {
            filters.add(Criteria.where("ingredients._id").is(search.getIngredientId().trim()));
        }
        if (search.getDiet() != null) {
            filters.add(Criteria.where("ingredients").not().elemMatch(
                Criteria.where("dietaryTags").ne(search.getDiet().name())));
        }
        if (search.getExcludeAllergen() != null) {
            filters.add(Criteria.where("ingredients").not().elemMatch(
                Criteria.where("allergens").is(search.getExcludeAllergen().name())));
        }
        if (search.getSpice() != null) {
            List<String> forbidden = new ArrayList<>();
            for (SpiceLevel level : SpiceLevel.values()) {
                if (level.getSeverity() > search.getSpice().getSeverity()) {
                    forbidden.add(level.name());
                }
            }
            if (!forbidden.isEmpty()) {
                filters.add(Criteria.where("ingredients").not().elemMatch(
                    Criteria.where("spiceLevel").in(forbidden)));
            }
            filters.add(Criteria.where("ingredients.spiceLevel").is(search.getSpice().name()));
        }
        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(
            filters.toArray(new Criteria[0]));
    }

    private void validate(TacoSearchCriteria search) {
        if (search.getPage() < 0 || search.getSize() < 1 || search.getSize() > 50) {
            throw new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Page must be non-negative and size must be between 1 and 50.");
        }
        if (!ALLOWED_SORTS.contains(search.getSort())) {
            throw new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Sort must be createdAt or name.");
        }
        String direction = search.getDirection().toLowerCase(Locale.ROOT);
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Direction must be asc or desc.");
        }
        if (hasText(search.getName()) && search.getName().trim().length() > 50) {
            throw new BadRequestException(ApiErrorCodes.VALIDATION_FAILED,
                "Name filter must not exceed 50 characters.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
//Fin TC-19
