package tacos.security;
import java.util.Locale;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Getter;
import lombok.Setter;
import tacos.User;

@Getter
@Setter
public class RegistrationForm {

  //modificacion para TC-10
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must contain between 3 and 50 characters")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
  private String password;

  @NotBlank(message = "Full name is required")
  @Size(max = 100, message = "Full name must not exceed 100 characters")
  private String fullname;
  private String street;
  private String city;
  private String state;
  private String zip;
  private String phone;

  @NotBlank(message = "Email is required")
  @Email(message = "Email has an invalid format")
  @Size(max = 120, message = "Email must not exceed 120 characters")
  private String email;
  
  public User toUser(PasswordEncoder passwordEncoder) { //modificacion para TC-10
    return new User(
        username.trim(), passwordEncoder.encode(password),
        fullname.trim(), street, city, state, zip, phone,
        email.trim().toLowerCase(Locale.ROOT));
  }
  
}
