package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import tacos.Ingredient;

//TC-18 - Exige exactamente una base wrap o bowl
@Component
public class BaseCountRule implements TacoDesignRule {
    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        long bases = context.getIngredients().stream()
            .filter(i -> i.getType() == Ingredient.Type.WRAP || i.getType() == Ingredient.Type.BOWL)
            .count();
        return bases == 1 ? Collections.emptyList() : Collections.singletonList(
            new RuleViolation("INVALID_BASE_COUNT", "A taco must contain exactly one base."));
    }
}
//Fin TC-18
