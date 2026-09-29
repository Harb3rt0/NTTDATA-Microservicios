package tacos.api.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import tacos.physics.RuleViolation;

//TC-18 - Resultado sin efectos de validar un taco
@Data
public class TacoValidationResponse {
    private boolean valid;
    private List<RuleViolation> violations = new ArrayList<>();
    private TacoClassificationResponse classification;
}
//Fin TC-18
