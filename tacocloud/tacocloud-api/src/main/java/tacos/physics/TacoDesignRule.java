package tacos.physics;

import java.util.List;

//TC-18 - Contrato componible de reglas de diseno
public interface TacoDesignRule {
    List<RuleViolation> validate(TacoDesignContext context);
}
//Fin TC-18
