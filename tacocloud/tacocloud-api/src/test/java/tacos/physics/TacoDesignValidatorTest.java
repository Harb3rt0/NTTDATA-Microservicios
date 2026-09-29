package tacos.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import tacos.Ingredient;
import tacos.SpiceLevel;

//TC-18 - Pruebas focalizadas de reglas componibles
public class TacoDesignValidatorTest {
    @Test
    public void shouldAcceptValidDesignWithoutViolations() {
        assertTrue(validator().validate(context(
            ingredient("BASE", Ingredient.Type.WRAP, true, SpiceLevel.NONE),
            ingredient("VEG", Ingredient.Type.VEGGIES, true, SpiceLevel.NONE))).isEmpty());
    }

    @Test
    public void shouldRejectZeroOrMultipleBases() {
        assertCodes(validator().validate(context(
            ingredient("VEG", Ingredient.Type.VEGGIES, true, SpiceLevel.NONE),
            ingredient("SAUCE", Ingredient.Type.SAUCE, true, SpiceLevel.NONE))),
            "INVALID_BASE_COUNT");
        assertCodes(validator().validate(context(
            ingredient("WRAP", Ingredient.Type.WRAP, true, SpiceLevel.NONE),
            ingredient("BOWL", Ingredient.Type.BOWL, true, SpiceLevel.NONE))),
            "INVALID_BASE_COUNT");
    }

    @Test
    public void shouldRejectIngredientCountOutsideRangeAndDuplicates() {
        assertCodes(validator().validate(context(
            ingredient("BASE", Ingredient.Type.WRAP, true, SpiceLevel.NONE))),
            "INGREDIENT_COUNT_OUT_OF_RANGE");
        Ingredient base = ingredient("BASE", Ingredient.Type.WRAP, true, SpiceLevel.NONE);
        assertCodes(validator().validate(new TacoDesignContext("Test", Arrays.asList(base, base))),
            "DUPLICATE_INGREDIENT", "INVALID_BASE_COUNT");
    }

    @Test
    public void shouldRejectUnavailableIngredient() {
        assertCodes(validator().validate(context(
            ingredient("BASE", Ingredient.Type.WRAP, true, SpiceLevel.NONE),
            ingredient("OFF", Ingredient.Type.VEGGIES, false, SpiceLevel.NONE))),
            "INGREDIENT_NOT_AVAILABLE");
    }

    @Test
    public void shouldApplyConfigurableFunRulesWithoutMagicIds() {
        List<RuleViolation> violations = validator().validate(context(
            ingredient("BASE", Ingredient.Type.WRAP, true, SpiceLevel.NONE),
            ingredient("HOT", Ingredient.Type.PROTEIN, true, SpiceLevel.HOT),
            ingredient("CHEESE", Ingredient.Type.CHEESE, true, SpiceLevel.NONE)));

        assertCodes(violations, "HOT_REQUIRES_BALANCE", "TYPE_PAIR_REQUIRED");
    }

    @Test
    public void shouldReturnAllViolationsForInvalidDesign() {
        Ingredient repeated = ingredient("BOWL", Ingredient.Type.BOWL, false, SpiceLevel.HOT);
        List<RuleViolation> violations = validator().validate(
            new TacoDesignContext("Invalid", Arrays.asList(repeated, repeated)));

        assertCodes(violations, "DUPLICATE_INGREDIENT", "HOT_REQUIRES_BALANCE",
            "INGREDIENT_NOT_AVAILABLE", "INGREDIENT_NOT_AVAILABLE", "INVALID_BASE_COUNT");
    }

    @Test
    public void shouldAllowAddingFakeRuleWithoutChangingValidator() {
        TacoDesignRule fake = context -> Collections.singletonList(
            new RuleViolation("FAKE_RULE", "Fake rule executed."));
        TacoDesignValidator validator = new TacoDesignValidator(Collections.singletonList(fake));

        assertCodes(validator.validate(context()), "FAKE_RULE");
    }

    private TacoDesignValidator validator() {
        return new TacoDesignValidator(Arrays.asList(new BaseCountRule(), new IngredientCountRule(),
            new DuplicateIngredientRule(), new AvailabilityRule(),
            new HotRequiresTypeRule(Ingredient.Type.VEGGIES),
            new TypePairRule(Ingredient.Type.CHEESE, Ingredient.Type.SAUCE)));
    }

    private TacoDesignContext context(Ingredient... ingredients) {
        return new TacoDesignContext("Test taco", Arrays.asList(ingredients));
    }

    private Ingredient ingredient(String id, Ingredient.Type type, boolean available, SpiceLevel spice) {
        Ingredient ingredient = new Ingredient(id, id, type, BigDecimal.ONE, available, 10, 2);
        ingredient.setSpiceLevel(spice);
        return ingredient;
    }

    private void assertCodes(List<RuleViolation> violations, String... expected) {
        assertEquals(Arrays.asList(expected), violations.stream().map(RuleViolation::getCode)
            .collect(Collectors.toList()));
    }
}
//Fin TC-18
