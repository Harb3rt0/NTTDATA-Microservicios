package tacos.api.mapper;

import java.math.BigDecimal;

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
        request.setUnitPrice(new BigDecimal("2.50")); //modificacion para TC-13
        request.setAvailable(true);
        request.setStockOnHand(100);
        request.setReorderLevel(20);

        IngredientMapper mapper = new IngredientMapper();
        Ingredient ingredient = mapper.toEntity(request);

        assertEquals("CARN", ingredient.getId());
        assertEquals("Carnitas",ingredient.getName());

        IngredientResponse response = mapper.toResponse(ingredient);
        assertEquals("CARN", response.getId());
        assertEquals("Carnitas", response.getName());
        assertEquals(Ingredient.Type.PROTEIN, response.getType());
        assertEquals(new BigDecimal("2.50"), response.getUnitPrice()); //modificacion para TC-13
        assertEquals(true, response.isAvailable());
    }
}
