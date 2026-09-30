package tacos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.yaml.snakeyaml.Yaml;

//TC-35 - contrato OpenAPI y compatibilidad de rutas
public class Tc35ApiContractTest {
  @Test
  public void shouldParseCompleteOpenApiWithoutSensitiveSchemas() throws Exception {
    InputStream source = getClass().getResourceAsStream("/static/openapi.yaml");
    Map<String, Object> document = new Yaml().load(source);
    assertThat(document).containsEntry("openapi", "3.0.3");
    @SuppressWarnings("unchecked")
    Map<String, Object> paths = (Map<String, Object>) document.get("paths");
    assertThat(paths).containsKeys("/ingredients", "/tacos", "/orders",
        "/users/me/favorites", "/users/me/orders", "/kitchen/queue",
        "/admin/announcements");
    String yaml = new String(getClass().getResourceAsStream("/static/openapi.yaml").readAllBytes());
    assertThat(yaml).doesNotContain("cardNumber", "cvv", "password", "authorities", "paymentToken");
  }

  @Test
  public void shouldRewriteV1AndDeprecateOnlyLegacyApi() throws Exception {
    ApiVersionAliasFilter filter = new ApiVersionAliasFilter();
    MockHttpServletRequest v1 = new MockHttpServletRequest("GET", "/api/v1/ingredients");
    MockHttpServletResponse v1Response = new MockHttpServletResponse();
    final String[] forwarded = new String[1];
    filter.doFilter(v1, v1Response, (request, response) ->
        forwarded[0] = ((javax.servlet.http.HttpServletRequest) request).getRequestURI());
    assertThat(forwarded[0]).isEqualTo("/api/ingredients");
    assertThat(v1Response.getHeader("Deprecation")).isNull();

    MockHttpServletResponse legacyResponse = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest("GET", "/api/ingredients"), legacyResponse,
        new MockFilterChain());
    assertThat(legacyResponse.getHeader("Deprecation")).isEqualTo("true");
    assertThat(legacyResponse.getHeader("Link")).contains("successor-version");
  }
}
//Fin TC-35
