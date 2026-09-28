package tacos.security;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.data.UserIndexService;
import tacos.data.UserRepository;

//TC-10 - Componer validación de unicidad, codificación y persistencia reactiva
@Service
public class RegistrationService {
  private final UserRepository userRepo;
  private final UserIndexService userIndexService;
  private final PasswordEncoder passwordEncoder;

  public RegistrationService(UserRepository userRepo, UserIndexService userIndexService, PasswordEncoder passwordEncoder) {
    this.userRepo = userRepo;
    this.userIndexService = userIndexService;
    this.passwordEncoder = passwordEncoder;
  }

  public Mono<RegistrationResponse> register(RegistrationForm form) {
    return userIndexService.ensureUniqueIndexes().then(Mono.defer(() -> {
      String username = form.getUsername().trim();
      String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

      return userRepo.findByUsername(username)
          .flatMap(user -> Mono.<RegistrationResponse>error(new UserAlreadyExistsException(
              RegistrationErrorCodes.USERNAME_ALREADY_EXISTS, "Username is already registered.")))
          .switchIfEmpty(Mono.defer(() -> userRepo.findByEmail(email)
              .flatMap(user -> Mono.<RegistrationResponse>error(new UserAlreadyExistsException(
                  RegistrationErrorCodes.EMAIL_ALREADY_EXISTS, "Email is already registered.")))
              .switchIfEmpty(Mono.defer(() -> userRepo.save(form.toUser(passwordEncoder))
                  .map(RegistrationResponse::fromUser)))));
    }));
  }
}
//Fin TC-10
