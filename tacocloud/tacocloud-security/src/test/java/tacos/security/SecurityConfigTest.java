package tacos.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import tacos.User;

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

  //TC-11 - Los roles persistidos se convierten en autoridades Spring Security
  @Test
  public void shouldExposeConfiguredRolesAndKeepUserAsLegacyDefault() {
    User privilegedUser = testUser("operator");
    privilegedUser.setRoles(new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_KITCHEN")));

    assertThat(privilegedUser.getAuthorities()).extracting("authority")
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_KITCHEN");

    User legacyUser = testUser("legacy");
    legacyUser.setRoles(null);
    assertThat(legacyUser.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
  }

  private User testUser(String username) {
    return new User(username, "{bcrypt}hash", "Test User", "Street", "City", "TX",
        "78701", "555-0100", username + "@example.com");
  }
  //Fin TC-11
}
