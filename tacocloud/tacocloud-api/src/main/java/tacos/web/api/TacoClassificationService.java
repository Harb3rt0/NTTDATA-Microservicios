package tacos.web.api;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.SpiceLevel;
import tacos.api.dto.TacoClassificationResponse;

//TC-17 - Deriva clasificacion solo desde ingredientes confiables
@Service
public class TacoClassificationService {
    public TacoClassificationResponse classify(List<Ingredient> ingredients) {
        Set<DietaryTag> tags = EnumSet.noneOf(DietaryTag.class);
        if (ingredients != null && !ingredients.isEmpty()) {
            for (DietaryTag tag : DietaryTag.values()) {
                if (ingredients.stream().allMatch(i -> safeTags(i).contains(tag))) {
                    tags.add(tag);
                }
            }
        }

        Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
        SpiceLevel spice = SpiceLevel.NONE;
        if (ingredients != null) {
            for (Ingredient ingredient : ingredients) {
                allergens.addAll(safeAllergens(ingredient));
                SpiceLevel candidate = ingredient.getSpiceLevel() == null
                    ? SpiceLevel.NONE : ingredient.getSpiceLevel();
                if (candidate.getSeverity() > spice.getSeverity()) {
                    spice = candidate;
                }
            }
        }

        TacoClassificationResponse response = new TacoClassificationResponse();
        response.setDietaryTags(tags);
        response.setAllergens(allergens);
        response.setSpiceLevel(spice);
        return response;
    }

    private Set<DietaryTag> safeTags(Ingredient ingredient) {
        return ingredient.getDietaryTags() == null
            ? EnumSet.noneOf(DietaryTag.class) : ingredient.getDietaryTags();
    }

    private Set<Allergen> safeAllergens(Ingredient ingredient) {
        return ingredient.getAllergens() == null
            ? EnumSet.noneOf(Allergen.class) : ingredient.getAllergens();
    }
}
//Fin TC-17
