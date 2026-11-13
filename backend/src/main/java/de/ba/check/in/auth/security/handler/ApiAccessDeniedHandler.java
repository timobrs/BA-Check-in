package de.ba.check.in.auth.security.handler;

import de.ba.check.in.api.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final ApiSecurityProblemWriter problemWriter;

    /** Erstellt eine neue Instanz. */
    public ApiAccessDeniedHandler(ApiSecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    /** Sendet eine Antwort bei fehlender Berechtigung. */
    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AccessDeniedException accessDeniedException)
            throws IOException {
        problemWriter.write(response, ApiErrorCode.ACCESS_DENIED);
    }
}
