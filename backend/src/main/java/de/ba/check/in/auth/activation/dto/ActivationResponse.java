package de.ba.check.in.auth.activation.dto;

import de.ba.check.in.users.domain.UserRole;

import java.util.UUID;

public record ActivationResponse(
        UUID userId,
        UserRole userRole,
        String displayName,
        UUID deviceId,
        UUID deviceCredentialId
) {
}
