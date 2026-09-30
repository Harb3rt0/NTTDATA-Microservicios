package tacos;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tacos.api.CorrelationIds;

//TC-31 - propaga el correlation id en el borde HTTP y limpia MDC
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String correlationId = CorrelationIds.resolve(request.getHeader(CorrelationIds.HEADER));
    request.setAttribute(CorrelationIds.ATTRIBUTE, correlationId);
    response.setHeader(CorrelationIds.HEADER, correlationId);
    try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
    }
  }
}
//Fin TC-31
