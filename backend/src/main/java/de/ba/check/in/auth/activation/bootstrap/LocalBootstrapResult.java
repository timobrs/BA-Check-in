package de.ba.check.in.auth.activation.bootstrap;

import java.time.Instant;
import java.util.UUID;

public record LocalBootstrapResult(
        UUID activationCodeId,
        Instant expiresAt,
        boolean newlyCreated
) {
}
