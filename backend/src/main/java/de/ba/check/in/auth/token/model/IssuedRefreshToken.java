package de.ba.check.in.auth.token.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IssuedRefreshToken(
        @NotBlank String refreshToken,
        @NotNull Instant expiresAt
) {
}
