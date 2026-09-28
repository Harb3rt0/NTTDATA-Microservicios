package tacos.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class SecurityConfigTest {

  @Test
  public void shouldEncodePasswordsWithDelegatingBcryptEncoder() {
    PasswordEncoder passwordEncoder = new SecurityConfig().encoder();
    String rawPassword = "TacoSecret123";

    String encodedPassword = passwordEncoder.encode(rawPassword);

    assertThat(encodedPassword).isNotEqualTo(rawPassword);
    assertThat(encodedPassword).startsWith("{bcrypt}");
    assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
  }
}
