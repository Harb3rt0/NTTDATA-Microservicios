package tacos.api.error;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component("apiAuthenticationEntryPoint")
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        ApiProblem problem = new ApiProblem(
            "urn:tacocloud:problem:unauthorized",
            "Authentication required",
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication is required to access this resource.",
            request.getRequestURI(),
            ApiErrorCodes.AUTHENTICATION_REQUIRED,
            Collections.emptyList()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(ApiExceptionHandler.PROBLEM_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
