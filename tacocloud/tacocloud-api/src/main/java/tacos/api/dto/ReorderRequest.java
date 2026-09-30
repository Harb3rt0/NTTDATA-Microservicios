package tacos.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

//TC-24 - Confirmacion, pago actual y clave de idempotencia
@Data
public class ReorderRequest {
    @NotBlank(message = "Payment method is required")
    @Size(max = 64, message = "Payment method identifier must not exceed 64 characters")
    private String paymentMethodId;

    private boolean confirmPriceChange;

    @NotBlank(message = "Reorder key is required")
    @Size(max = 64, message = "Reorder key must not exceed 64 characters")
    private String reorderKey;
}
//Fin TC-24
