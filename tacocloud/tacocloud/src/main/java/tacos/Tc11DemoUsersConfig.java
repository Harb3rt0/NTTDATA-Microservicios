package tacos;

import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.data.UserRepository;

//TC-11 - Usuarios ficticios para pruebas locales de autorización
@Configuration
@Profile("tc11-demo")
public class Tc11DemoUsersConfig {

  private static final Logger log = LoggerFactory.getLogger(Tc11DemoUsersConfig.class);

  @Bean
  public ApplicationRunner tc11DemoUsers(UserRepository userRepo, PasswordEncoder passwordEncoder,
      @Value("${tacocloud.tc11-demo.password:}") String password) {

    return args -> {
      if (password == null || password.length() < 8) {
        throw new IllegalStateException(
            "TACOCLOUD_TC11_DEMO_PASSWORD must contain at least 8 characters.");
      }

      prepareUsers(userRepo, passwordEncoder, password)
          .doOnSuccess(unused -> log.info("TC-11 demo users are ready."))
          .subscribe(
              unused -> { },
              error -> log.error("Could not prepare TC-11 demo users.", error));
    };
  }

  Mono<Void> prepareUsers(UserRepository userRepo, PasswordEncoder passwordEncoder, String password) {
    return Flux.concat(
        prepareUser(userRepo, passwordEncoder, password,
            "user-a", "Usuario A TC11", "user-a@example.test", "ROLE_USER"),
        prepareUser(userRepo, passwordEncoder, password,
            "user-b", "Usuario B TC11", "user-b@example.test", "ROLE_USER"),
        prepareUser(userRepo, passwordEncoder, password,
            "admin-test", "Administrador TC11", "admin@example.test", "ROLE_ADMIN"),
        prepareUser(userRepo, passwordEncoder, password,
            "kitchen-test", "Cocina TC11", "kitchen@example.test", "ROLE_KITCHEN"))
        .then();
  }

  private Mono<User> prepareUser(UserRepository userRepo, PasswordEncoder passwordEncoder,
      String password, String username, String fullname, String email, String role) {

    return userRepo.findByUsername(username)
        .map(existingUser -> demoUser(existingUser.getId(), passwordEncoder, password,
            username, fullname, email, role))
        .switchIfEmpty(Mono.defer(() -> Mono.just(
            demoUser(null, passwordEncoder, password, username, fullname, email, role))))
        .flatMap(userRepo::save);
  }

  private User demoUser(String id, PasswordEncoder passwordEncoder, String password,
      String username, String fullname, String email, String role) {

    User user = new User(username, passwordEncoder.encode(password), fullname,
        "Calle Pruebas 100", "Guadalajara", "Jalisco", "44100", "5550101000", email);
    user.setId(id);
    user.setRoles(new HashSet<>(Collections.singleton(role)));
    return user;
  }
}
//Fin TC-11
