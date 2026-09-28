package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Mono;
import tacos.data.UserRepository;

//TC-11 - Verificar usuarios ficticios, roles y contraseñas codificadas
public class Tc11DemoUsersConfigTest {

  @Test
  public void shouldPrepareDemoUsersWithExpectedRolesAndEncodedPasswords() throws Exception {
    UserRepository userRepo = org.mockito.Mockito.mock(UserRepository.class);
    PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    Tc11DemoUsersConfig config = new Tc11DemoUsersConfig();

    when(userRepo.findByUsername(anyString())).thenReturn(Mono.empty());
    when(passwordEncoder.encode("DemoPassword123"))
        .thenAnswer(invocation -> "{bcrypt}" + invocation.getArgument(0));
    when(userRepo.save(any(User.class)))
        .thenAnswer(invocation -> Mono.just(invocation.<User>getArgument(0)));

    config.prepareUsers(userRepo, passwordEncoder, "DemoPassword123").toFuture().get();

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo, times(4)).save(userCaptor.capture());
    verify(passwordEncoder, times(4)).encode("DemoPassword123");

    List<User> savedUsers = userCaptor.getAllValues();
    Map<String, User> usersByUsername = savedUsers.stream()
        .collect(Collectors.toMap(User::getUsername, Function.identity()));

    assertThat(usersByUsername).containsOnlyKeys("user-a", "user-b", "admin-test", "kitchen-test");
    assertThat(usersByUsername.get("user-a").getRoles()).containsExactly("ROLE_USER");
    assertThat(usersByUsername.get("user-b").getRoles()).containsExactly("ROLE_USER");
    assertThat(usersByUsername.get("admin-test").getRoles()).containsExactly("ROLE_ADMIN");
    assertThat(usersByUsername.get("kitchen-test").getRoles()).containsExactly("ROLE_KITCHEN");
    assertThat(savedUsers).allSatisfy(user -> {
      assertThat(user.getPassword()).startsWith("{bcrypt}");
      assertThat(user.getPassword()).isNotEqualTo("DemoPassword123");
    });
  }
}
//Fin TC-11
