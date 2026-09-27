package tacos.api.dto;

import lombok.Data;
import tacos.Ingredient;

@Data 
public class IngredientResponse {
    private String id;
    private String name;
    private Ingredient.Type type;
}
