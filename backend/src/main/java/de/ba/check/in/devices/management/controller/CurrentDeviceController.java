package de.ba.check.in.devices.management.controller;

import de.ba.check.in.devices.management.service.CurrentDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices/current")
@RequiredArgsConstructor
@Tag(name = "Current device")
@SecurityRequirement(name = "bearerAuth")
public class CurrentDeviceController {
    private final CurrentDeviceService currentDeviceService;

    /** Löst das Gerät vom Konto. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unlink the current device", description =
            "Permanently revokes the authenticated device, its credentials, challenges, and refresh tokens.")
    public void unlink(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        UUID credentialId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("credential_id")));
        Number sessionVersion = Objects.requireNonNull(jwt.getClaim("session_version"));
        currentDeviceService.unlink(userId, credentialId, sessionVersion.longValue());
    }
}
