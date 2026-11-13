package de.ba.check.in.auth.session.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class DeviceRefreshProof {

    /** Erstellt eine neue Instanz. */
    private DeviceRefreshProof() {
    }

    /** Erstellt die Nutzdaten. */
    public static byte[] payload(UUID credentialId, byte[] refreshTokenHash, long timestampEpochMillis) {
        String payload = String.join("\n",
                "BACHECKIN_DEVICE_REFRESH_V1",
                credentialId.toString(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(refreshTokenHash),
                Long.toString(timestampEpochMillis));
        return payload.getBytes(StandardCharsets.UTF_8);
    }
}
