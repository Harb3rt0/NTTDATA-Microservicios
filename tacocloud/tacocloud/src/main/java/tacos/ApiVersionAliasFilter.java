package tacos;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//TC-35 - alias compatible de api v1 y deprecacion del contrato legado
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiVersionAliasFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri.startsWith("/api/v1/")) {
      String alias = "/api/" + uri.substring("/api/v1/".length());
      chain.doFilter(new HttpServletRequestWrapper(request) {
        @Override public String getRequestURI() { return alias; }
        @Override public String getServletPath() { return alias; }
      }, response);
      return;
    }
    if (uri.startsWith("/api/")) {
      response.setHeader("Deprecation", "true");
      response.setHeader("Link", "</api/v1>; rel=successor-version");
    }
    chain.doFilter(request, response);
  }
}
//Fin TC-35
