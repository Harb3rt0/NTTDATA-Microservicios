package tacos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tacos.data.UserRepository;

//TC-11 - Autenticación Basic real con usuarios y roles almacenados en Mongo
@SpringBootTest(properties = {
    "spring.boot.admin.client.enabled=false",
    "management.info.git.enabled=false",
    "management.info.build.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class Tc11DemoUsersAuthenticationIntegrationTest {

  private static final String DEMO_PASSWORD = "DemoPassword123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  public void prepareDemoUsers() throws Exception {
    new Tc11DemoUsersConfig()
        .prepareUsers(userRepo, passwordEncoder, DEMO_PASSWORD)
        .toFuture().get();
  }

  @Test
  public void shouldAuthenticatePersistedUserWithBasicAuth() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/orders")
            .with(httpBasic("user-a", DEMO_PASSWORD)))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());

    User user = userRepo.findByUsername("user-a").toFuture().get();
    assertThat(user.getRoles()).containsExactly("ROLE_USER");
    assertThat(passwordEncoder.matches(DEMO_PASSWORD, user.getPassword())).isTrue();
  }

  @Test
  public void shouldApplyPersistedAdminAndKitchenRoles() throws Exception {
    mockMvc.perform(get("/actuator/info").with(httpBasic("admin-test", DEMO_PASSWORD)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/actuator/info").with(httpBasic("kitchen-test", DEMO_PASSWORD)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
//Fin TC-11
