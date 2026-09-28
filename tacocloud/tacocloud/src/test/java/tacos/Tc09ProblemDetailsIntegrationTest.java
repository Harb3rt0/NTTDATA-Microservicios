package tacos;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.boot.admin.client.enabled=false",
    "management.info.git.enabled=false", "management.info.build.enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
public class Tc09ProblemDetailsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void invalidOrderUsesProblemDetailsContract() throws Exception {
    mockMvc.perform(post("/api/orders").with(user("alice").roles("USER")) //modificacion para TC-11
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.instance").value("/api/orders"))
        .andExpect(jsonPath("$.violations.length()", greaterThan(1)));
  }

  @Test
  public void unknownApiRouteDoesNotFallThroughToSpa() throws Exception {
    mockMvc.perform(get("/api/does-not-exist").with(user("alice").roles("USER"))) //modificacion para TC-11
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.instance").value("/api/does-not-exist"));
  }
}
