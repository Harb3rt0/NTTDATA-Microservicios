package tacos.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

//TC-15 - Respuesta economica de una cotizacion
@Data
public class OrderQuoteResponse {
    private List<OrderLineResponse> items = new ArrayList<>();
    private BigDecimal subtotalBeforeDiscount;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String currency;
    private String couponCode;
}
//Fin TC-15
