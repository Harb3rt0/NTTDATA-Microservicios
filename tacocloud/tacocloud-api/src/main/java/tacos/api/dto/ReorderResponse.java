package tacos.api.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-24 - Resultado o solicitud de confirmacion de recompra
@Data
@AllArgsConstructor
public class ReorderResponse {
    private String originalOrderId;
    private boolean priceChanged;
    private boolean requiresConfirmation;
    private BigDecimal originalTotal;
    private BigDecimal currentTotal;
    private String currency;
    private String message;
    private OrderResponse order;
}
//Fin TC-24
