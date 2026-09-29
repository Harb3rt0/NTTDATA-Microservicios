package tacos.api.dto;

import java.util.Set;

import lombok.Data;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

//TC-17 - Clasificacion derivada de un taco
@Data
public class TacoClassificationResponse {
    private Set<DietaryTag> dietaryTags;
    private Set<Allergen> allergens;
    private SpiceLevel spiceLevel;
    private String disclaimer = "Academic metadata; it is not a cross-contamination certification.";
}
//Fin TC-17
