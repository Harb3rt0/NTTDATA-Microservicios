package tacos.api.dto;

import java.math.BigDecimal;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

//TC-13 - Campos permitidos para actualizar el catalogo
@Getter
@Setter
public class IngredientCatalogUpdateRequest {
    @DecimalMin(value = "0.00", message = "Ingredient unit price must not be negative")
    private BigDecimal unitPrice;

    private Boolean available;

    @Min(value = 0, message = "Ingredient reorder level must not be negative")
    private Integer reorderLevel;

    @JsonIgnore
    @AssertTrue(message = "At least one catalog field is required")
    public boolean isCatalogUpdatePresent() {
        return unitPrice != null || available != null || reorderLevel != null;
    }
}
//Fin TC-13
