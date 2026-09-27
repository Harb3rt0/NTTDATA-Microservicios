package tacos.api.error;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    public static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ApiViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiViolation(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

        return validationProblem(request, violations);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiProblem> handleBinding(BindException ex, HttpServletRequest request) {
        List<ApiViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiViolation(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

        return validationProblem(request, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiProblem> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ApiViolation> violations = ex.getConstraintViolations().stream()
            .map(violation -> new ApiViolation(
                violation.getPropertyPath().toString(), violation.getMessage()))
            .collect(Collectors.toList());

        return validationProblem(request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblem> handleInvalidJson(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "urn:tacocloud:problem:invalid-json", "Invalid JSON",
            "The request body could not be read.", ApiErrorCodes.INVALID_JSON, request,
            Collections.emptyList());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiProblem> handleInvalidRequestValue(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "urn:tacocloud:problem:bad-request", "Bad request",
            "A request parameter or path value is invalid.", ApiErrorCodes.BAD_REQUEST, request,
            Collections.emptyList());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiProblem> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "urn:tacocloud:problem:bad-request", "Bad request",
            ex.getMessage(), ex.getCode(), request, Collections.emptyList());
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiProblem> handleNotFound(Exception ex, HttpServletRequest request) {
        String code = ex instanceof ResourceNotFoundException
            ? ((ResourceNotFoundException) ex).getCode()
            : ApiErrorCodes.RESOURCE_NOT_FOUND;
        String detail = ex instanceof ResourceNotFoundException
            ? ex.getMessage()
            : "The requested resource was not found.";

        return problem(HttpStatus.NOT_FOUND, "urn:tacocloud:problem:not-found", "Resource not found",
            detail, code, request, Collections.emptyList());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiProblem> handleBusinessRule(BusinessRuleException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "urn:tacocloud:problem:business-rule",
            "Business rule violation", ex.getMessage(), ex.getCode(), request, Collections.emptyList());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiProblem> handleConflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:tacocloud:problem:conflict", "Conflict",
            ex.getMessage(), ex.getCode(), request, Collections.emptyList());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiProblem> handleDuplicateKey(DuplicateKeyException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:tacocloud:problem:conflict", "Conflict",
            "A resource with the same unique value already exists.", ApiErrorCodes.DUPLICATE_RESOURCE,
            request, Collections.emptyList());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiProblem> handleOptimisticLock(OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:tacocloud:problem:conflict", "Conflict",
            "The resource was modified by another operation.", ApiErrorCodes.OPTIMISTIC_LOCK_CONFLICT,
            request, Collections.emptyList());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiProblem> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "urn:tacocloud:problem:method-not-allowed",
            "Method not allowed", "The HTTP method is not supported for this resource.",
            ApiErrorCodes.METHOD_NOT_ALLOWED, request, Collections.emptyList());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiProblem> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "urn:tacocloud:problem:unsupported-media-type",
            "Unsupported media type", "The request content type is not supported.",
            ApiErrorCodes.UNSUPPORTED_MEDIA_TYPE, request, Collections.emptyList());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblem> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "urn:tacocloud:problem:forbidden", "Access denied",
            "You are not allowed to perform this operation.", ApiErrorCodes.ACCESS_DENIED, request,
            Collections.emptyList());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiProblem> handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "urn:tacocloud:problem:unauthorized",
            "Authentication required", "Authentication is required to access this resource.",
            ApiErrorCodes.AUTHENTICATION_REQUIRED, request, Collections.emptyList());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleUnexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "urn:tacocloud:problem:internal",
            "Internal server error", "An unexpected error occurred.", ApiErrorCodes.INTERNAL_ERROR,
            request, Collections.emptyList());
    }

    private ResponseEntity<ApiProblem> validationProblem(HttpServletRequest request,
            List<ApiViolation> violations) {
        return problem(HttpStatus.BAD_REQUEST, "urn:tacocloud:problem:validation",
            "Request validation failed", "One or more fields are invalid.",
            ApiErrorCodes.VALIDATION_FAILED, request, violations);
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String type, String title,
            String detail, String code, HttpServletRequest request, List<ApiViolation> violations) {
        ApiProblem problem = new ApiProblem(type, title, status.value(), detail, request.getRequestURI(),
            code, violations == null ? Collections.emptyList() : violations);

        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(problem);
    }
}
