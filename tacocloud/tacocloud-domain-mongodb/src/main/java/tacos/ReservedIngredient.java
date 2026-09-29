package tacos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//TC-16 - Cantidad reservada por ingrediente
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservedIngredient {
    private String ingredientId;
    private int quantity;
}
//Fin TC-16
