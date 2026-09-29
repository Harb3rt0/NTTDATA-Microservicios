package tacos.physics;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

//TC-18 - Rechaza ingredientes no disponibles
@Component
public class AvailabilityRule implements TacoDesignRule {
    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        return context.getIngredients().stream()
            .filter(i -> !i.isAvailable())
            .map(i -> new RuleViolation("INGREDIENT_NOT_AVAILABLE",
                "Ingredient '" + i.getId() + "' is not available."))
            .collect(Collectors.toList());
    }
}
//Fin TC-18
