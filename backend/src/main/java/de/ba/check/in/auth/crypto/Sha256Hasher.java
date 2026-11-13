package de.ba.check.in.auth.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class Sha256Hasher {

    /** Berechnet den Hash eines ASCII-Werts. */
    public byte[] hashAscii(String value) {
        return hash(value.getBytes(StandardCharsets.US_ASCII));
    }

    /** Berechnet einen Hash. */
    public byte[] hash(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
