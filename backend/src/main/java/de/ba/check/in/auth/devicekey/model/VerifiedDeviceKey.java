package de.ba.check.in.auth.devicekey.model;

import java.util.Arrays;

public record VerifiedDeviceKey(
        String publicKeyBase64,
        byte[] publicKeyFingerprint,
        String algorithm
) {

    /** Erstellt eine neue Instanz. */
    public VerifiedDeviceKey {
        publicKeyFingerprint = Arrays.copyOf(publicKeyFingerprint, publicKeyFingerprint.length);
    }

    /** Berechnet den Fingerabdruck des öffentlichen Schlüssels. */
    @Override
    public byte[] publicKeyFingerprint() {
        return Arrays.copyOf(publicKeyFingerprint, publicKeyFingerprint.length);
    }
}
