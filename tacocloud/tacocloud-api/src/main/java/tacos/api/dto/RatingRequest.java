package tacos.api.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

//TC-22 - Entrada validada de calificacion
@Data
public class RatingRequest {
    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be between 1 and 5")
    @Max(value = 5, message = "Score must be between 1 and 5")
    private Integer score;
}
//Fin TC-22
