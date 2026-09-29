package tacos.physics;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

//TC-18 - Rechaza ingredientes repetidos
@Component
public class DuplicateIngredientRule implements TacoDesignRule {
    @Override
    public List<RuleViolation> validate(TacoDesignContext context) {
        Set<String> ids = new HashSet<>();
        boolean duplicate = context.getIngredients().stream().anyMatch(i -> !ids.add(i.getId()));
        return duplicate ? Collections.singletonList(new RuleViolation(
            "DUPLICATE_INGREDIENT", "A taco cannot contain duplicate ingredients."))
            : Collections.emptyList();
    }
}
//Fin TC-18
