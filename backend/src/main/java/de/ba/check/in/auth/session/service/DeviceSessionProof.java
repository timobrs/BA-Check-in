package de.ba.check.in.auth.session.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class DeviceSessionProof {

    /** Erstellt eine neue Instanz. */
    private DeviceSessionProof() {
    }

    /** Erstellt die Nutzdaten. */
    public static byte[] payload(UUID credentialId, UUID authSessionId, byte[] challenge, long timestampEpochMillis) {
        String payload = String.join("\n",
                "BACHECKIN_DEVICE_SESSION_V1",
                credentialId.toString(),
                authSessionId.toString(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge),
                Long.toString(timestampEpochMillis));
        return payload.getBytes(StandardCharsets.UTF_8);
    }
}
