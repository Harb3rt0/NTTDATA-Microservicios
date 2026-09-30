package tacos.api.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-23 - Resumen seguro para historial
@Data
@AllArgsConstructor
public class OrderSummaryResponse {
    private String id;
    private Date placedAt;
    private int itemCount;
    private BigDecimal total;
    private String currency;
}
//Fin TC-23
