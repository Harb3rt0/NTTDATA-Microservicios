package tacos.api.dto;

import java.math.BigDecimal;

import lombok.Data;

//TC-14 - Respuesta de una linea con snapshot economico
@Data
public class OrderLineResponse {
    private TacoResponse taco;
    private int quantity;
    private BigDecimal unitPriceAtPurchase;
    private BigDecimal subtotal;
}
//Fin TC-14
