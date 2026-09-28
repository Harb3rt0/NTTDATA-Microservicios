package tacos.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import tacos.User;

//TC-10 - Respuesta de registro sin contraseña ni campos internos
@Data
@AllArgsConstructor
public class RegistrationResponse {
  private String username;
  private String fullname;
  private String email;

  public static RegistrationResponse fromUser(User user) {
    return new RegistrationResponse(user.getUsername(), user.getFullname(), user.getEmail());
  }
}
//Fin TC-10
