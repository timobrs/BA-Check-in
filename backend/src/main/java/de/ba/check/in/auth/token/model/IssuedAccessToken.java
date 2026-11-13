package de.ba.check.in.auth.token.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IssuedAccessToken(
        @NotBlank String accessToken,
        @NotNull Instant expiresAt
) {
}
