package tacos.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tacos.Ingredient;
import tacos.api.dto.IngredientRequest;
import tacos.api.dto.IngredientResponse;

public class IngredientMapperTest {
    @Test
    public void shouldMapIngredientRequestAndResponse() {
        IngredientRequest request = new IngredientRequest();
        request.setId("CARN");
        request.setName("Carnitas");
        request.setType(Ingredient.Type.PROTEIN);

        IngredientMapper mapper = new IngredientMapper();
        Ingredient ingredient = mapper.toEntity(request);

        assertEquals("CARN", ingredient.getId());
        assertEquals("Carnitas",ingredient.getName());

        IngredientResponse response = mapper.toResponse(ingredient);
        assertEquals("CARN", response.getId());
        assertEquals("Carnitas", response.getName());
        assertEquals(Ingredient.Type.PROTEIN, response.getType());
    }
}
