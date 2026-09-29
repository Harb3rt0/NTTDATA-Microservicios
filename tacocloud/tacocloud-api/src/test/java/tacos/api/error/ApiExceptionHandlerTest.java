package tacos.api.error;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

public class ApiExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestErrorController(), new ApiNotFoundController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    public void shouldReturnAllValidationViolationsAsProblemJson() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deliveryName\":\"\",\"deliveryZip\":\"1\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:tacocloud:problem:validation"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.VALIDATION_FAILED))
            .andExpect(jsonPath("$.instance").value("/test/validation"))
            .andExpect(jsonPath("$.violations.length()").value(3))
            .andExpect(jsonPath("$.violations[*].field",
                containsInAnyOrder("deliveryName", "deliveryCity", "deliveryZip")));
    }

    @Test
    public void shouldReturnNotFoundAsProblemJson() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.ORDER_NOT_FOUND))
            .andExpect(jsonPath("$.instance").value("/test/not-found"));
    }

    @Test
    public void shouldKeepUnknownApiRouteOutOfSpaFallback() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.RESOURCE_NOT_FOUND))
            .andExpect(jsonPath("$.instance").value("/api/does-not-exist"));
    }

    @Test
    public void shouldReturnConflictAsProblemJson() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.DUPLICATE_RESOURCE));
    }

    //TC-13 - Traduce conflictos de version al contrato ApiProblem
    @Test
    public void shouldReturnOptimisticLockConflictAsProblemJson() throws Exception {
        mockMvc.perform(get("/test/optimistic-lock"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.OPTIMISTIC_LOCK_CONFLICT))
            .andExpect(jsonPath("$.detail").value(
                "The resource was modified by another operation."));
    }
    //Fin TC-13

    @Test
    public void shouldReturnBusinessRuleAsUnprocessableEntity() throws Exception {
        mockMvc.perform(get("/test/business"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND));
    }

    @Test
    public void shouldNotExposeStackTraceOrDriverMessage() throws Exception {
        mockMvc.perform(get("/test/internal"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.INTERNAL_ERROR))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(content().string(not(containsString("MongoCommandException"))))
            .andExpect(content().string(not(containsString("secret driver message"))));
    }

    @Test
    public void shouldReturnInvalidJsonWithoutParserDetails() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deliveryName\":"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(ApiExceptionHandler.PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.INVALID_JSON))
            .andExpect(jsonPath("$.detail").value("The request body could not be read."));
    }

    @Test
    public void shouldMapAuthorizationErrors() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.ACCESS_DENIED));

        mockMvc.perform(get("/test/unauthorized"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ApiErrorCodes.AUTHENTICATION_REQUIRED));
    }

    @RestController
    static class TestErrorController {
        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new ResourceNotFoundException(ApiErrorCodes.ORDER_NOT_FOUND, "Order was not found.");
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ConflictException(ApiErrorCodes.DUPLICATE_RESOURCE,
                "Order is in a conflicting state.");
        }

        //TC-13 - Simula la excepcion real entregada por Spring Data
        @GetMapping("/test/optimistic-lock")
        public void optimisticLock() {
            throw new OptimisticLockingFailureException("internal stale version detail");
        }
        //Fin TC-13

        @GetMapping("/test/business")
        public void business() {
            throw new BusinessRuleException(ApiErrorCodes.ORDER_INGREDIENT_NOT_FOUND,
                "Order violates a business rule.");
        }

        @GetMapping("/test/internal")
        public void internal() {
            throw new RuntimeException("MongoCommandException secret driver message");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("internal policy name");
        }

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new BadCredentialsException("internal authentication detail");
        }
    }

    static class ValidationRequest {
        @NotBlank(message = "Delivery name is required")
        public String deliveryName;

        @NotBlank(message = "Delivery city is required")
        public String deliveryCity;

        @Size(min = 3, max = 12, message = "Delivery ZIP has an invalid size")
        public String deliveryZip;
    }
}
