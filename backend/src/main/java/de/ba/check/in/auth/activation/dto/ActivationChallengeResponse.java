package de.ba.check.in.auth.activation.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivationChallengeResponse(
        UUID activationSessionId,
        String challengeBase64Url,
        Instant expiresAt
) {
}
