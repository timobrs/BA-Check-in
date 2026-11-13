package de.ba.check.in.auth.activation.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ActivationCodeGenerator {
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int GROUP_COUNT = 4;
    private static final int GROUP_LENGTH = 4;

    private final SecureRandom secureRandom;

    /** Erstellt eine neue Instanz. */
    public ActivationCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /** Erzeugt einen neuen Wert. */
    public String generate() {
        StringBuilder result = new StringBuilder(GROUP_COUNT * GROUP_LENGTH + GROUP_COUNT - 1);
        for (int group = 0; group < GROUP_COUNT; group++) {
            if (group > 0) {
                result.append('-');
            }
            for (int index = 0; index < GROUP_LENGTH; index++) {
                result.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
            }
        }
        return result.toString();
    }
}
