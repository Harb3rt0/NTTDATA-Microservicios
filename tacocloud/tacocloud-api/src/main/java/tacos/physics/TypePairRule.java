package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tacos.Ingredient;

//TC-18 - Regla configurable de pareja entre tipos
@Component
public class TypePairRule implements TacoDesignRule {
    private final Ingredient.Type triggerType;
    private final Ingredient.Type requiredType;

    public TypePairRule(@Value("${tacocloud.taco-rules.pair.trigger-type:CHEESE}") Ingredient.Type triggerType,
            @Value("${tacocloud.taco-rules.pair.required-type:SAUCE}") Ingredient.Type requiredType) {
        this.triggerType = triggerType;
        this.requiredType = requiredType;
    }

    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        boolean triggered = context.getIngredients().stream().anyMatch(i -> i.getType() == triggerType);
        boolean paired = context.getIngredients().stream().anyMatch(i -> i.getType() == requiredType);
        return !triggered || paired ? Collections.emptyList() : Collections.singletonList(
            new RuleViolation("TYPE_PAIR_REQUIRED",
                triggerType + " requires an ingredient of type " + requiredType + "."));
    }
}
//Fin TC-18
