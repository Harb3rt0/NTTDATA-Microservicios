package tacos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import tacos.api.CorrelationIds;

//TC-31 - generacion, preservacion, saneamiento y limpieza MDC
public class Tc31CorrelationIdFilterTest {
  @Test
  public void shouldGenerateAndReturnUuidWhenHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ingredients");
    MockHttpServletResponse response = new MockHttpServletResponse();
    new CorrelationIdFilter().doFilter(request, response, new MockFilterChain());
    assertThat(response.getHeader(CorrelationIds.HEADER)).matches("[0-9a-f-]{36}");
  }

  @Test
  public void shouldPreserveValidValueAndCleanMdc() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ingredients");
    request.addHeader(CorrelationIds.HEADER, "client.CORR-1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> during = new AtomicReference<>();
    new CorrelationIdFilter().doFilter(request, response, (req, res) ->
        during.set(MDC.get("correlationId")));
    assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo("client.CORR-1");
    assertThat(during.get()).isEqualTo("client.CORR-1");
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  public void shouldReplaceMaliciousOrOversizedValues() {
    assertThat(CorrelationIds.resolve("bad\r\nFORGED")).doesNotContain("FORGED");
    assertThat(CorrelationIds.resolve("a".repeat(65))).hasSize(36);
  }
}
//Fin TC-31
