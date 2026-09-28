package tacos.security;

import org.springframework.dao.DuplicateKeyException;

import lombok.Getter;

//TC-10 - Representar conflictos conocidos de unicidad de usuario
@Getter
public class UserAlreadyExistsException extends DuplicateKeyException {
  private static final long serialVersionUID = 1L;

  private final String code;

  public UserAlreadyExistsException(String code, String message) {
    super(message);
    this.code = code;
  }
}
//Fin TC-10
