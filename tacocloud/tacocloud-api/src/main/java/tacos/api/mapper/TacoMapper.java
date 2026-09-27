package tacos.api.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.Taco;
import tacos.api.dto.TacoCreateRequest;
import tacos.api.dto.TacoResponse;

@Component 
public class TacoMapper {
    private final IngredientMapper ingredientMapper;

    public TacoMapper(IngredientMapper ingredientMapper) {
        this.ingredientMapper = ingredientMapper;
    }

    public Taco toEntity(TacoCreateRequest request, List<Ingredient> ingredients) {
        Taco taco = new Taco();
        taco.setName(request.getName());
        taco.setIngredients(ingredients);
        return taco;
    }

    public TacoResponse toResponse(Taco taco) {
        TacoResponse response = new TacoResponse();
        response.setName(taco.getName());
        response.setIngredients(taco.getIngredients().stream()
            .map(ingredientMapper::toResponse)
            .collect(Collectors.toList())
        );

        return response;
    }
}