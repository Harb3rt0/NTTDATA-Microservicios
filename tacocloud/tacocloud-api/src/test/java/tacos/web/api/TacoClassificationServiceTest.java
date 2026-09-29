package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.SpiceLevel;
import tacos.api.dto.TacoClassificationResponse;

//TC-17 - Pruebas de clasificacion derivada
public class TacoClassificationServiceTest {
    private final TacoClassificationService service = new TacoClassificationService();

    @Test
    public void shouldClassifyTacoAsVeganOnlyWhenAllIngredientsAreVegan() {
        TacoClassificationResponse vegan = service.classify(Arrays.asList(
            ingredient("A", EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE),
            ingredient("B", EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.MILD)));
        TacoClassificationResponse mixed = service.classify(Arrays.asList(
            ingredient("A", EnumSet.allOf(DietaryTag.class), EnumSet.noneOf(Allergen.class), SpiceLevel.NONE),
            ingredient("MEAT", EnumSet.of(DietaryTag.GLUTEN_FREE),
                EnumSet.noneOf(Allergen.class), SpiceLevel.NONE)));

        assertTrue(vegan.getDietaryTags().contains(DietaryTag.VEGAN));
        assertFalse(mixed.getDietaryTags().contains(DietaryTag.VEGAN));
        assertFalse(mixed.getDietaryTags().contains(DietaryTag.VEGETARIAN));
    }

    @Test
    public void shouldCombineAllergensUsingExactUnion() {
        TacoClassificationResponse result = service.classify(Arrays.asList(
            ingredient("A", EnumSet.noneOf(DietaryTag.class),
                EnumSet.of(Allergen.GLUTEN), SpiceLevel.NONE),
            ingredient("B", EnumSet.noneOf(DietaryTag.class),
                EnumSet.of(Allergen.DAIRY), SpiceLevel.NONE)));

        assertEquals(EnumSet.allOf(Allergen.class), result.getAllergens());
    }

    @Test
    public void shouldCalculateSpiceLevelDeterministically() {
        TacoClassificationResponse result = service.classify(Arrays.asList(
            ingredient("HOT", EnumSet.noneOf(DietaryTag.class),
                EnumSet.noneOf(Allergen.class), SpiceLevel.HOT),
            ingredient("MILD", EnumSet.noneOf(DietaryTag.class),
                EnumSet.noneOf(Allergen.class), SpiceLevel.MILD)));

        assertEquals(SpiceLevel.HOT, result.getSpiceLevel());
    }

    @Test
    public void shouldDeriveGlutenFreeOnlyWhenAllIngredientsQualify() {
        TacoClassificationResponse result = service.classify(Arrays.asList(
            ingredient("GF", EnumSet.of(DietaryTag.GLUTEN_FREE),
                EnumSet.noneOf(Allergen.class), SpiceLevel.NONE),
            ingredient("GLUTEN", EnumSet.noneOf(DietaryTag.class),
                EnumSet.of(Allergen.GLUTEN), SpiceLevel.NONE)));

        assertFalse(result.getDietaryTags().contains(DietaryTag.GLUTEN_FREE));
    }

    private Ingredient ingredient(String id, java.util.Set<DietaryTag> tags,
            java.util.Set<Allergen> allergens, SpiceLevel spice) {
        return new Ingredient(id, id, Ingredient.Type.VEGGIES, BigDecimal.ONE,
            true, 10, 2, tags, allergens, spice);
    }
}
//Fin TC-17
