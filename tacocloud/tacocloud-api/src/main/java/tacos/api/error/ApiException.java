package tacos.api.error;

import lombok.Getter;

@Getter 
public abstract class ApiException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }
}
