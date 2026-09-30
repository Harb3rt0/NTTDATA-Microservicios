package tacos.api.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import tacos.Ingredient;
import tacos.Taco;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.dto.TacoResponse;
import tacos.web.api.TacoClassificationService;

@Component 
public class TacoMapper {
    private final IngredientMapper ingredientMapper;
    private final TacoClassificationService classificationService;

    @Autowired
    public TacoMapper(IngredientMapper ingredientMapper,
            TacoClassificationService classificationService) { //modificacion para TC-17
        this.ingredientMapper = ingredientMapper;
        this.classificationService = classificationService;
    }

    public TacoMapper(IngredientMapper ingredientMapper) {
        this(ingredientMapper, new TacoClassificationService());
    }

    public Taco toEntity(TacoCreateRequest request, List<Ingredient> ingredients) {
        Taco taco = new Taco();
        taco.setName(request.getName());
        taco.setIngredients(ingredients);
        return taco;
    }

    public TacoResponse toResponse(Taco taco) {
        TacoResponse response = new TacoResponse();
        //modificacion para TC-19
        response.setId(taco.getId());
        response.setCreatedAt(taco.getCreatedAt());
        response.setName(taco.getName());
        response.setIngredients(taco.getIngredients().stream()
            .map(ingredientMapper::toResponse)
            .collect(Collectors.toList())
        );
        response.setClassification(classificationService.classify(taco.getIngredients()));

        return response;
    }
}
