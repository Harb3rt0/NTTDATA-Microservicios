package tacos.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import tacos.Ingredient;

@Data 
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngredientRequest {    //modificacion para TC-09
    @NotBlank(message = "Ingredient id is required")
    @Size(max = 20, message = "Ingredient id must not exceed 20 characters")
    private String id;

    @NotBlank(message = "Ingredient name is required")
    @Size(max = 50, message = "Ingredient name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Ingredient type is required")
    private Ingredient.Type type;
}
