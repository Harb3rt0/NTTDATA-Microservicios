package tacos.api.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

//TC-20 - Recomendacion diaria explicable
@Data
@AllArgsConstructor
public class TacoOfTheDayResponse {
    private TacoResponse taco;
    private LocalDate date;
    private String reason;
}
//Fin TC-20
