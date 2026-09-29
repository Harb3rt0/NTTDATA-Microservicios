package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

//TC-18 - Limita el diseno de dos a doce ingredientes
@Component
public class IngredientCountRule implements TacoDesignRule {
    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        int count = context.getIngredients().size();
        return count >= 2 && count <= 12 ? Collections.emptyList() : Collections.singletonList(
            new RuleViolation("INGREDIENT_COUNT_OUT_OF_RANGE",
                "A taco must contain between 2 and 12 ingredients."));
    }
}
//Fin TC-18
