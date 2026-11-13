package de.ba.check.in.auth.security.handler;

import de.ba.check.in.api.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class ApiSecurityProblemWriter {
    private static final String TRACE_HEADER = "X-Trace-Id";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Erstellt eine neue Instanz. */
    public ApiSecurityProblemWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Schreibt die Sicherheitsfehlermeldung. */
    public void write(HttpServletResponse response, ApiErrorCode error) throws IOException {
        String traceId = UUID.randomUUID().toString();
        ProblemDetail problem = ProblemDetail.forStatus(error.status());
        problem.setType(URI.create("urn:ba-check-in:error:" + error.name().toLowerCase().replace('_', '-')));
        problem.setTitle(error.title());
        problem.setDetail(error.detail());
        problem.setProperty("code", error.name());
        problem.setProperty("traceId", traceId);
        problem.setProperty("timestamp", Instant.now(clock));
        response.setStatus(error.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(TRACE_HEADER, traceId);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
