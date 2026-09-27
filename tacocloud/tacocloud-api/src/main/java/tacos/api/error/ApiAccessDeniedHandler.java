package tacos.api.error;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component("apiAccessDeniedHandler")
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        ApiProblem problem = new ApiProblem(
            "urn:tacocloud:problem:forbidden",
            "Access denied",
            HttpStatus.FORBIDDEN.value(),
            "You are not allowed to perform this operation.",
            request.getRequestURI(),
            ApiErrorCodes.ACCESS_DENIED,
            Collections.emptyList()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(ApiExceptionHandler.PROBLEM_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
