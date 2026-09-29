package tacos.api.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
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

    //TC-13 - Datos requeridos al crear o reemplazar un ingrediente
    @NotNull(message = "Ingredient unit price is required")
    @DecimalMin(value = "0.00", message = "Ingredient unit price must not be negative")
    private BigDecimal unitPrice;

    @NotNull(message = "Ingredient availability is required")
    private Boolean available;

    @NotNull(message = "Ingredient stock is required")
    @Min(value = 0, message = "Ingredient stock must not be negative")
    private Integer stockOnHand;

    @NotNull(message = "Ingredient reorder level is required")
    @Min(value = 0, message = "Ingredient reorder level must not be negative")
    private Integer reorderLevel;
    //Fin TC-13
}
