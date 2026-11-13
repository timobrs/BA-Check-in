package de.ba.check.in.auth.activation.controller;

import de.ba.check.in.auth.activation.dto.CreateActivationCodeRequest;
import de.ba.check.in.auth.activation.dto.CreatedActivationCodeResponse;
import de.ba.check.in.auth.activation.service.ActivationCodeService;
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
@RequestMapping("/api/v1/secretary")
@RequiredArgsConstructor
@Tag(name = "Secretary device administration")
@SecurityRequirement(name = "bearerAuth")
public class ActivationCodeController {
    private final ActivationCodeService activationCodeService;

    /** Erstellt einen Aktivierungscode. */
    @PostMapping("/users/{userId}/device-activation-codes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARY')")
    @Operation(summary = "Create a device activation code", description = """
            Creates a short-lived, single-use code for an existing user. Every role may
            have only one active device. Use `ADD_DEVICE` without an active device and
            `REPLACE_DEVICE` to replace the current device. Omitting the mode selects it automatically.
            """)
    public CreatedActivationCodeResponse createCode(@PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateActivationCodeRequest request) {
        UUID secretaryId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return request.mode() == null
                ? activationCodeService.createAutomaticActivationCode(userId, secretaryId)
                : activationCodeService.createActivationCode(userId, secretaryId, request.mode());
    }
}
