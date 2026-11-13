package de.ba.check.in.auth.activation.bootstrap;

import java.time.Instant;
import java.util.UUID;

public record ProductionBootstrapResult(UUID secretaryUserId, String activationCode, Instant expiresAt) {}
