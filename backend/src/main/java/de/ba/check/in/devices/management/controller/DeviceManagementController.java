package de.ba.check.in.devices.management.controller;

import de.ba.check.in.devices.management.dto.RevokeDeviceRequest;
import de.ba.check.in.devices.management.service.DeviceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretary/devices")
@RequiredArgsConstructor
@Tag(name = "Secretary device administration")
@SecurityRequirement(name = "bearerAuth")
public class DeviceManagementController {
    private final DeviceManagementService deviceManagementService;

    /** Sperrt das Gerät. */
    @PostMapping("/{deviceId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SECRETARY')")
    @Operation(summary = "Revoke a device", description =
            "Revokes the selected device and all credentials, challenges, and refresh tokens attached to it.")
    public void revoke(@PathVariable UUID deviceId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RevokeDeviceRequest request) {
        UUID secretaryId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        deviceManagementService.revoke(deviceId, secretaryId, request.reason());
    }
}
