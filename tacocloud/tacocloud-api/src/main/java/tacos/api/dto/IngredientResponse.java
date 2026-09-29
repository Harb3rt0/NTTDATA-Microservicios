package tacos.api.dto;

import java.math.BigDecimal;

import lombok.Data;
import tacos.Ingredient;

@Data 
public class IngredientResponse {
    private String id;
    private String name;
    private Ingredient.Type type;
    private BigDecimal unitPrice; //modificacion para TC-13
    private boolean available; //modificacion para TC-13
}
