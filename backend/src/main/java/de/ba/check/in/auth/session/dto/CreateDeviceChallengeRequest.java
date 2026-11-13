package de.ba.check.in.auth.session.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDeviceChallengeRequest(
        @NotNull(message = "deviceCredentialId is required")
        UUID deviceCredentialId
) {
}
