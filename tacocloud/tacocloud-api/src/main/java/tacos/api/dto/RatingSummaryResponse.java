package tacos.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-22 - Resumen agregado sin exponer votos individuales
@Data
@AllArgsConstructor
public class RatingSummaryResponse {
    private TacoResponse taco;
    private BigDecimal average;
    private long votes;
    private Map<Integer, Long> distribution;
}
//Fin TC-22
