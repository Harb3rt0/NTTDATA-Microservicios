package tacos.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import tacos.Ingredient;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngredientRequest {
    private String id;
    private String name;
    private Ingredient.Type type;
}
