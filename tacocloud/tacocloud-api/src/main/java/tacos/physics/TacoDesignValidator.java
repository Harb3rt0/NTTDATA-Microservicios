package tacos.physics;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

//TC-18 - Ejecuta una coleccion extensible de reglas
@Service
public class TacoDesignValidator {
    private final List<TacoDesignRule> rules;

    public TacoDesignValidator(List<TacoDesignRule> rules) {
        this.rules = rules;
    }

    public List<RuleViolation> validate(TacoDesignContext context) {
        return rules.stream().flatMap(rule -> rule.validate(context).stream())
            .sorted(Comparator.comparing(RuleViolation::getCode)
                .thenComparing(RuleViolation::getMessage))
            .collect(Collectors.toList());
    }

    public void requireValid(TacoDesignContext context) {
        List<RuleViolation> violations = validate(context);
        if (!violations.isEmpty()) {
            throw new TacoDesignException(violations);
        }
    }
}
//Fin TC-18
