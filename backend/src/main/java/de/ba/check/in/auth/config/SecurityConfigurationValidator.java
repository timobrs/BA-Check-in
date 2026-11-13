package de.ba.check.in.auth.config;

import java.time.Duration;

public final class SecurityConfigurationValidator {

    /** Erstellt eine neue Instanz. */
    private SecurityConfigurationValidator() {
    }

    /** Prüft auf einen positiven Wert. */
    public static Duration requirePositive(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(propertyName + " must be positive");
        }
        return value;
    }

    /** Prüft auf einen nichtnegativen Wert. */
    public static Duration requireNotNegative(Duration value, String propertyName) {
        if (value == null || value.isNegative()) {
            throw new IllegalStateException(propertyName + " must not be negative");
        }
        return value;
    }

    /** Prüft auf einen nichtleeren Text. */
    public static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
        return value;
    }
}
