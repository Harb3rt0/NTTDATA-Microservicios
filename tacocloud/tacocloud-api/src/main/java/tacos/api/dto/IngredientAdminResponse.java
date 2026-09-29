package tacos.api.dto;

import java.math.BigDecimal;

import lombok.Data;
import tacos.Ingredient;

//TC-13 - Respuesta administrativa con metadata operativa
@Data
public class IngredientAdminResponse {
    private String id;
    private String name;
    private Ingredient.Type type;
    private BigDecimal unitPrice;
    private boolean available;
    private int stockOnHand;
    private int reorderLevel;
    private Long version;
}
//Fin TC-13
