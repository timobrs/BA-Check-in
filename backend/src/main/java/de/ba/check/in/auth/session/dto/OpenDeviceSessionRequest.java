package de.ba.check.in.auth.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OpenDeviceSessionRequest(
        @NotNull(message = "Device credential ID is required")
        UUID deviceCredentialId,

        @NotNull(message = "Authentication session ID is required")
        UUID authSessionId,

        @Positive(message = "Timestamp must be positive")
        long timestampEpochMillis,

        @NotBlank(message = "Signature is required")
        @Size(max = 1024, message = "Signature is too large")
        String signatureBase64
) {
}
