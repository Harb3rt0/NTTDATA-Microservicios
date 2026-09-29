package tacos.physics;

import java.util.List;

import lombok.Getter;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;

//TC-18 - Error con todas las violaciones del diseno
@Getter
public class TacoDesignException extends BusinessRuleException {
    private static final long serialVersionUID = 1L;
    private final List<RuleViolation> violations;

    public TacoDesignException(List<RuleViolation> violations) {
        super(ApiErrorCodes.INVALID_TACO_DESIGN, "The taco design violates one or more rules.");
        this.violations = violations;
    }
}
//Fin TC-18
