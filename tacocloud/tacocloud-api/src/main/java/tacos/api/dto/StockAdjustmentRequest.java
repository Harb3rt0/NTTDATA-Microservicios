package tacos.api.dto;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

//TC-13 - Operacion explicita de entrada o salida de stock
@Getter
@Setter
public class StockAdjustmentRequest {
    @NotNull(message = "Stock adjustment is required")
    private Integer adjustment;
}
//Fin TC-13
