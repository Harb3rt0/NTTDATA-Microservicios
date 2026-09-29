package tacos.api.dto;

import java.math.BigDecimal;
import java.util.Set;

import lombok.Data;
import tacos.Ingredient;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

@Data 
public class IngredientResponse {
    private String id;
    private String name;
    private Ingredient.Type type;
    private BigDecimal unitPrice; //modificacion para TC-13
    private boolean available; //modificacion para TC-13
    //TC-17 - Metadata academica visible del catalogo
    private Set<DietaryTag> dietaryTags;
    private Set<Allergen> allergens;
    private SpiceLevel spiceLevel;
    //Fin TC-17
}
