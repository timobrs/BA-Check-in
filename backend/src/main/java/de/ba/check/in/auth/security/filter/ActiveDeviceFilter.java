package de.ba.check.in.auth.security.filter;

import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ActiveDeviceFilter extends OncePerRequestFilter {
    private final DeviceCredentialRepository repository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /** Erstellt eine neue Instanz. */
    public ActiveDeviceFilter(DeviceCredentialRepository repository,
                             @Qualifier("bearerAuthenticationEntryPoint") AuthenticationEntryPoint authenticationEntryPoint) {
        this.repository = repository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /** Prüft die Anfrage vor der Weitergabe. */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String value = jwtAuthenticationToken.getToken().getClaimAsString("credential_id");
            String userValue = jwtAuthenticationToken.getToken().getSubject();
            String deviceValue = jwtAuthenticationToken.getToken().getClaimAsString("device_id");
            String roleValue = jwtAuthenticationToken.getToken().getClaimAsString("role");
            Object sessionVersionClaim = jwtAuthenticationToken.getToken().getClaim("session_version");
            boolean active;
            try {
                active = value != null
                        && userValue != null
                        && deviceValue != null
                        && roleValue != null
                        && sessionVersionClaim instanceof Number sessionVersion
                        && repository.isCredentialActive(
                                UUID.fromString(value),
                                sessionVersion.longValue(),
                                UUID.fromString(userValue),
                                UUID.fromString(deviceValue),
                                UserRole.valueOf(roleValue));
            } catch (IllegalArgumentException exception) {
                active = false;
            }
            if (!active) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response,
                        new BadCredentialsException("Device is not active."));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
