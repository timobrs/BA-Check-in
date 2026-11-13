package de.ba.check.in.auth.session.model;

import java.util.Arrays;
import java.util.UUID;

public record ClaimedDeviceChallenge(
        UUID credentialId,
        UUID authSessionId,
        String publicKeyBase64,
        byte[] challenge,
        long timestampEpochMillis
) {

    /** Erstellt eine neue Instanz. */
    public ClaimedDeviceChallenge {
        challenge = Arrays.copyOf(challenge, challenge.length);
    }

    /** Gibt die Challenge zurück. */
    @Override
    public byte[] challenge() {
        return Arrays.copyOf(challenge, challenge.length);
    }
}
