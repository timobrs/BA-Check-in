package de.ba.check.in.auth.session.dto;

import java.time.Instant;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {
}
