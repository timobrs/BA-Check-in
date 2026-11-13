package de.ba.check.in.auth.activation.service;

import java.util.Arrays;
import java.util.UUID;

public record ActivationPreparation(
        UUID userId,
        UUID activationCodeId,
        byte[] challenge
) {

    /** Erstellt eine neue Instanz. */
    public ActivationPreparation {
        challenge = Arrays.copyOf(challenge, challenge.length);
    }

    /** Gibt die Challenge zurück. */
    @Override
    public byte[] challenge() {
        return Arrays.copyOf(challenge, challenge.length);
    }
}
