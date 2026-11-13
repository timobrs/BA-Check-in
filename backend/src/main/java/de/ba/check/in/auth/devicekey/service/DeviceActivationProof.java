package de.ba.check.in.auth.devicekey.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class DeviceActivationProof {

    /** Erstellt eine neue Instanz. */
    private DeviceActivationProof() {
    }

    /** Erstellt die Nutzdaten. */
    public static byte[] payload(UUID activationSessionId, byte[] challenge, byte[] publicKeyFingerprint) {
        String payload = String.join(
                "\n",
                "BACHECKIN_DEVICE_ACTIVATION_V1",
                activationSessionId.toString(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge),
                Base64.getUrlEncoder().withoutPadding().encodeToString(publicKeyFingerprint));
        return payload.getBytes(StandardCharsets.UTF_8);
    }
}
