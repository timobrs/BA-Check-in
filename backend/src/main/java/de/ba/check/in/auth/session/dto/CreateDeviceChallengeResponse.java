package de.ba.check.in.auth.session.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateDeviceChallengeResponse(
        UUID authSessionId,
        String challengeBase64Url,
        Instant expiresAt
) {
}
