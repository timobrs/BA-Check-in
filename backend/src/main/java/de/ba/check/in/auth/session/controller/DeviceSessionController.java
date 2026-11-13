package de.ba.check.in.auth.session.controller;

import de.ba.check.in.auth.session.dto.*;
import de.ba.check.in.auth.session.service.DeviceSessionService;
import de.ba.check.in.auth.ratelimit.DatabaseSecurityRateLimiter;
import de.ba.check.in.auth.ratelimit.SecurityRateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-sessions")
@RequiredArgsConstructor
@Tag(name = "Device sessions")
public class DeviceSessionController {
    private final DeviceSessionService deviceSessionService;
    private final DatabaseSecurityRateLimiter rateLimiter;

    /** Erstellt eine Challenge. */
    @PostMapping("/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a login challenge", description =
            "Creates a short-lived, single-use challenge foran active device credential.")
    public CreateDeviceChallengeResponse createChallenge(@Valid @RequestBody CreateDeviceChallengeRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.check(SecurityRateLimitPolicy.DEVICE_CHALLENGE,
                servletRequest.getRemoteAddr(),
                request.deviceCredentialId().toString());
        return deviceSessionService.createChallenge(request.deviceCredentialId());
    }

    /** Öffnet eine Sitzung. */
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a device session", description = """
            Sign the UTF-8 bytes of these newline-separated values without a trailing newline:
            `BACHECKIN_DEVICE_SESSION_V1`, device credential UUID, authentication session UUID,
            challenge as unpadded Base64URL, and the decimal Unix timestamp in milliseconds.
            A successful request returns a short-lived JWT and a rotating refresh token.
            """)
    public TokenPairResponse openSession(@Valid @RequestBody OpenDeviceSessionRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.check(SecurityRateLimitPolicy.SESSION_OPEN,
                servletRequest.getRemoteAddr(),
                request.deviceCredentialId().toString());
        return deviceSessionService.openSession(request);
    }

    /** Erneuert die Sitzung. */
    @PostMapping("/refresh")
    @Operation(summary = "Rotate a device session", description = """
            Sign the UTF-8 bytes of these newline-separated values without a trailing newline:
            `BACHECKIN_DEVICE_REFRESH_V1`, device credential UUID, the SHA-256 hash of the raw
            refresh token as unpadded Base64URL, and the decimal Unix timestamp in milliseconds.
            Refresh tokens are single-use; replay revokes the complete token family.
            """)
    public TokenPairResponse refresh(@Valid @RequestBody RefreshDeviceSessionRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.check(SecurityRateLimitPolicy.TOKEN_REFRESH,
                servletRequest.getRemoteAddr(),
                request.deviceCredentialId().toString());
        return deviceSessionService.refresh(request);
    }

    /** Beendet die Sitzung. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out the current device session", description =
            "Revokes all refresh tokens and invalidates all access tokens for the current credential.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        UUID authenticationCredentialId =
                UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("credential_id")));
        Number sessionVersion = Objects.requireNonNull(jwt.getClaim("session_version"));
        deviceSessionService.logout(authenticationCredentialId, sessionVersion.longValue());
    }
}
