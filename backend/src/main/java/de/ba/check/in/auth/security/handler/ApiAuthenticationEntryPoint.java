package de.ba.check.in.auth.security.handler;

import de.ba.check.in.api.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("bearerAuthenticationEntryPoint")
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ApiSecurityProblemWriter problemWriter;

    /** Erstellt eine neue Instanz. */
    public ApiAuthenticationEntryPoint(ApiSecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    /** Sendet die Antwort bei fehlender Anmeldung. */
    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AuthenticationException authenticationException)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        problemWriter.write(response, ApiErrorCode.DEVICE_AUTHENTICATION_FAILED);
    }
}
