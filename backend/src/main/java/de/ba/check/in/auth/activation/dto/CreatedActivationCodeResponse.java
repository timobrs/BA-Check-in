package de.ba.check.in.auth.activation.dto;

import de.ba.check.in.auth.activation.domain.ActivationMode;

import java.time.Instant;
import java.util.UUID;

public record CreatedActivationCodeResponse(
        UUID activationCodeId,
        UUID userId,
        String activationCode,
        Instant expiresAt,
        ActivationMode mode
) {
}
