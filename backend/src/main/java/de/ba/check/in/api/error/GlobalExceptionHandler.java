package de.ba.check.in.api.error;

import de.ba.check.in.application.service.CalendarImportValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_HEADER = "X-Trace-Id";

    private final Clock clock;

    /** Erstellt eine neue Instanz. */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /** Behandelt einen API-Fehler. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        ApiErrorCode error = exception.getErrorCode();
        String traceId = traceId();
        log.warn("API request failed: code={}, path={}, traceId={}, reason={}",
                error.name(), request.getRequestURI(), traceId, exception.getMessage());
        ResponseEntity<ProblemDetail> response = response(error, traceId, List.of());
        if (exception instanceof CalendarImportValidationException && response.getBody() != null) {
            response.getBody().setDetail(exception.getMessage());
        }
        if (exception instanceof RateLimitedException rateLimitedException) {
            Instant now = clock.instant();
            long retryAfterSeconds =
                    Math.max(1, (rateLimitedException.getRetryAt().toEpochMilli() - now.toEpochMilli() + 999) / 1000);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(response.getHeaders());
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
            ProblemDetail body = response.getBody();
            if (body != null) {
                body.setProperty("retryAt", rateLimitedException.getRetryAt());
            }
            return new ResponseEntity<>(body, headers, response.getStatusCode());
        }
        return response;
    }

    /** Behandelt Validierungsfehler. */
    @ExceptionHandler({MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleValidation(Exception exception, HttpServletRequest request) {
        String traceId = traceId();
        log.debug("Request validation failed: path={}, traceId={}", request.getRequestURI(), traceId);
        return response(ApiErrorCode.VALIDATION_FAILED, traceId, validationErrors(exception));
    }

    /** Behandelt eine ungültige Anfrage. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(HttpServletRequest request) {
        String traceId = traceId();
        log.debug("Malformed request: path={}, traceId={}", request.getRequestURI(), traceId);
        return response(ApiErrorCode.MALFORMED_REQUEST, traceId, List.of());
    }

    /** Behandelt einen unerwarteten Fehler. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        String traceId = traceId();
        log.error("Unhandled request failure: path={}, traceId={}", request.getRequestURI(), traceId, exception);
        return response(ApiErrorCode.INTERNAL_ERROR, traceId, List.of());
    }

    /** Erstellt die Antwort. */
    private ResponseEntity<ProblemDetail> response(ApiErrorCode error,
            String traceId,
            List<Map<String, String>> validationErrors) {
        ProblemDetail problem = ProblemDetail.forStatus(error.status());
        problem.setType(URI.create("urn:ba-check-in:error:" + error.name().toLowerCase().replace('_', '-')));
        problem.setTitle(error.title());
        problem.setDetail(error.detail());
        problem.setProperty("code", error.name());
        problem.setProperty("traceId", traceId);
        problem.setProperty("timestamp", Instant.now(clock));
        if (!validationErrors.isEmpty()) {
            problem.setProperty("errors", validationErrors);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(TRACE_HEADER, traceId);
        return new ResponseEntity<>(problem, headers, error.status());
    }

    /** Sammelt die Validierungsfehler. */
    private List<Map<String, String>> validationErrors(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            return validationException.getBindingResult().getFieldErrors().stream().map(error -> Map.of("field",
                    error.getField(),
                    "message",
                    error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage())).toList();
        }
        if (exception instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream().map(violation -> Map.of("field",
                    violation.getPropertyPath().toString(),
                    "message",
                    violation.getMessage())).toList();
        }
        return List.of();
    }

    /** Gibt die Trace-ID zurück. */
    private String traceId() {
        return UUID.randomUUID().toString();
    }
}
