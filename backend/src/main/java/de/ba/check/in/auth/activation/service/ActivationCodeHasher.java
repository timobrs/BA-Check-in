package de.ba.check.in.auth.activation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;

@Component
public class ActivationCodeHasher {
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    /** Erstellt eine neue Instanz. */
    public ActivationCodeHasher(@Value("${app.security.activation-code.hmac-secret}") String encodedSecret) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Activation-code HMAC secret must be valid Base64", exception);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("Activation-code HMAC secret must contain at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret, ALGORITHM);
    }

    /** Berechnet einen Hash. */
    public byte[] hash(String activationCode) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(normalize(activationCode).getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    /** Vereinheitlicht die Eingabe. */
    public String normalize(String activationCode) {
        return activationCode.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
