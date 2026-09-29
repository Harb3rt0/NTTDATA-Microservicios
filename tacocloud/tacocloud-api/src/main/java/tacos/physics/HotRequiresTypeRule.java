package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.SpiceLevel;

//TC-18 - Regla configurable para balancear tacos hot
@Component
public class HotRequiresTypeRule implements TacoDesignRule {
    private final Ingredient.Type requiredType;

    public HotRequiresTypeRule(
            @Value("${tacocloud.taco-rules.hot-requires-type:VEGGIES}") Ingredient.Type requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        boolean hot = context.getIngredients().stream()
            .anyMatch(i -> i.getSpiceLevel() == SpiceLevel.HOT);
        boolean balanced = context.getIngredients().stream()
            .anyMatch(i -> i.getType() == requiredType);
        return !hot || balanced ? Collections.emptyList() : Collections.singletonList(
            new RuleViolation("HOT_REQUIRES_BALANCE",
                "A HOT taco requires an ingredient of type " + requiredType + "."));
    }
}
//Fin TC-18
