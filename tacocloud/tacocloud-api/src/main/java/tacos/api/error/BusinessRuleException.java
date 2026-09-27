package tacos.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessRuleException extends ApiException{
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String code,String message) {
        super(code, message);
    }
}
