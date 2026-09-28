package tacos;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Mono;
import tacos.data.UserRepository;
import tacos.security.RegistrationErrorCodes;

@SpringBootTest(properties = "spring.boot.admin.client.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class Tc10RegistrationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockBean
  private UserRepository userRepo;

  @BeforeEach
  public void setUp() {
    reset(userRepo);
  }

  @Test
  public void shouldRegisterAfterPersistenceWithoutExposingPassword() throws Exception {
    when(userRepo.findByUsername("postmanuser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("postman@example.com")).thenReturn(Mono.empty());
    when(userRepo.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    MvcResult result = performRegistration(validJson("postmanuser", "postman@example.com"));

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value("postmanuser"))
        .andExpect(jsonPath("$.fullname").value("Postman User"))
        .andExpect(jsonPath("$.email").value("postman@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.authorities").doesNotExist());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());
    String storedPassword = userCaptor.getValue().getPassword();
    org.assertj.core.api.Assertions.assertThat(storedPassword).startsWith("{bcrypt}");
    org.assertj.core.api.Assertions.assertThat(storedPassword).isNotEqualTo("TacoSecret123");
    org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("TacoSecret123", storedPassword)).isTrue();
  }

  @Test
  public void shouldReturnConflictForDuplicateUsername() throws Exception {
    when(userRepo.findByUsername("existing")).thenReturn(Mono.just(existingUser()));

    MvcResult result = performRegistration(validJson("existing", "new@example.com"));

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value(RegistrationErrorCodes.USERNAME_ALREADY_EXISTS))
        .andExpect(jsonPath("$.instance").value("/register"));

    verify(userRepo, never()).findByEmail(any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldReturnConflictForDuplicateEmail() throws Exception {
    when(userRepo.findByUsername("newuser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("existing@example.com")).thenReturn(Mono.just(existingUser()));

    MvcResult result = performRegistration(validJson("newuser", "existing@example.com"));

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value(RegistrationErrorCodes.EMAIL_ALREADY_EXISTS))
        .andExpect(jsonPath("$.instance").value("/register"));

    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldTranslateDatabaseDuplicateRaceToConflict() throws Exception {
    when(userRepo.findByUsername("raceuser")).thenReturn(Mono.empty());
    when(userRepo.findByEmail("race@example.com")).thenReturn(Mono.empty());
    when(userRepo.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("index details")));

    MvcResult result = performRegistration(validJson("raceuser", "race@example.com"));

    mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
        .andExpect(jsonPath("$.detail").value("A resource with the same unique value already exists."));
  }

  @Test
  public void shouldValidateBeforeRepositoryInteraction() throws Exception {
    mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"\",\"password\":\"short\",\"fullname\":\"\",\"email\":\"invalid\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.violations[*].field",
            containsInAnyOrder("username", "username", "password", "fullname", "email")));

    verifyNoInteractions(userRepo);
  }

  private MvcResult performRegistration(String json) throws Exception {
    return mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(request().asyncStarted())
        .andReturn();
  }

  private String validJson(String username, String email) {
    return "{"
        + "\"username\":\"" + username + "\","
        + "\"password\":\"TacoSecret123\","
        + "\"fullname\":\"Postman User\","
        + "\"street\":\"123 Main Street\","
        + "\"city\":\"Austin\","
        + "\"state\":\"TX\","
        + "\"zip\":\"78701\","
        + "\"phone\":\"555-0100\","
        + "\"email\":\"" + email + "\""
        + "}";
  }

  private User existingUser() {
    return new User("existing", "{bcrypt}hash", "Existing User", "Street", "City", "TX",
        "78701", "555-0101", "existing@example.com");
  }
}
