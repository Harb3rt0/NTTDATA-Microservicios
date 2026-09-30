package tacos.api.dto;

import lombok.Data;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

//TC-19 - Filtros y paginacion segura del catalogo
@Data
public class TacoSearchCriteria {
    private String name;
    private String ingredientId;
    private DietaryTag diet;
    private Allergen excludeAllergen;
    private SpiceLevel spice;
    private int page;
    private int size;
    private String sort;
    private String direction;
}
//Fin TC-19
