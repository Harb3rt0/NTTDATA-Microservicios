package tacos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import tacos.User;
import tacos.data.UserIndexService;
import tacos.data.UserRepository;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

  @Mock
  private UserRepository userRepo;

  @Mock
  private UserIndexService userIndexService;

  private PasswordEncoder passwordEncoder;
  private RegistrationService registrationService;

  @BeforeEach
  public void setUp() {
    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    when(userIndexService.ensureUniqueIndexes()).thenReturn(Mono.empty());
    registrationService = new RegistrationService(userRepo, userIndexService, passwordEncoder);
  }

  @Test
  public void shouldPersistOnceAndRespondOnlyAfterSaveCompletes() {
    RegistrationForm form = validForm("newuser", "newuser@example.com");
    Sinks.One<User> savedUser = Sinks.one();
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    when(userRepo.findByUsername("newuser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("newuser@example.com")).thenReturn(Mono.empty());
    when(userRepo.save(any(User.class))).thenReturn(savedUser.asMono());

    Mono<RegistrationResponse> registration = registrationService.register(form);
    verifyNoInteractions(userRepo);

    StepVerifier.create(registration)
        .then(() -> {
          verify(userRepo).save(userCaptor.capture());
          User persistedUser = userCaptor.getValue();
          assertThat(persistedUser.getPassword()).isNotEqualTo(form.getPassword());
          assertThat(persistedUser.getPassword()).startsWith("{bcrypt}");
          assertThat(passwordEncoder.matches(form.getPassword(), persistedUser.getPassword())).isTrue();
          savedUser.tryEmitValue(persistedUser);
        })
        .assertNext(response -> {
          assertThat(response.getUsername()).isEqualTo("newuser");
          assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        })
        .verifyComplete();

    verify(userRepo).save(any(User.class));
  }

  @Test
  public void shouldRejectDuplicateUsernameBeforeCheckingEmailOrSaving() {
    RegistrationForm form = validForm("existing", "new@example.com");
    when(userRepo.findByUsername("existing")).thenReturn(Mono.just(existingUser()));

    StepVerifier.create(registrationService.register(form))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(UserAlreadyExistsException.class);
          assertThat(((UserAlreadyExistsException) error).getCode())
              .isEqualTo(RegistrationErrorCodes.USERNAME_ALREADY_EXISTS);
        })
        .verify();

    verify(userRepo, never()).findByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldRejectDuplicateEmailBeforeSaving() {
    RegistrationForm form = validForm("newuser", "existing@example.com");
    when(userRepo.findByUsername("newuser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("existing@example.com")).thenReturn(Mono.just(existingUser()));

    StepVerifier.create(registrationService.register(form))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(UserAlreadyExistsException.class);
          assertThat(((UserAlreadyExistsException) error).getCode())
              .isEqualTo(RegistrationErrorCodes.EMAIL_ALREADY_EXISTS);
        })
        .verify();

    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldPropagateDatabaseDuplicateForRaceConditionMapping() {
    RegistrationForm form = validForm("racinguser", "race@example.com");
    when(userRepo.findByUsername("racinguser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("race@example.com")).thenReturn(Mono.empty());
    when(userRepo.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("duplicate index")));

    StepVerifier.create(registrationService.register(form))
        .expectError(DuplicateKeyException.class)
        .verify();

    verify(userRepo).save(any(User.class));
  }

  private RegistrationForm validForm(String username, String email) {
    RegistrationForm form = new RegistrationForm();
    form.setUsername(username);
    form.setPassword("TacoSecret123");
    form.setFullname("Taco User");
    form.setStreet("123 Main Street");
    form.setCity("Austin");
    form.setState("TX");
    form.setZip("78701");
    form.setPhone("555-0100");
    form.setEmail(email);
    return form;
  }

  private User existingUser() {
    return new User("existing", "{bcrypt}hash", "Existing User", "Street", "City", "TX",
        "78701", "555-0101", "existing@example.com");
  }
}
