package de.ba.check.in.auth.ratelimit;

import java.time.Duration;

public enum SecurityRateLimitPolicy {
    ACTIVATION_CHALLENGE(10, Duration.ofMinutes(10), Duration.ofMinutes(15)),
    ACTIVATION_COMPLETE(10, Duration.ofMinutes(10), Duration.ofMinutes(15)),
    DEVICE_CHALLENGE(30, Duration.ofMinutes(1), Duration.ofMinutes(5)),
    SESSION_OPEN(15, Duration.ofMinutes(1), Duration.ofMinutes(10)),
    TOKEN_REFRESH(30, Duration.ofMinutes(1), Duration.ofMinutes(10)),
    NFC_DEVICE(10, Duration.ofMinutes(1), Duration.ofMinutes(1)),
    NFC_CARD(300, Duration.ofMinutes(1), Duration.ofMinutes(1));

    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;

    /** Erstellt eine neue Instanz. */
    SecurityRateLimitPolicy(
            int maxAttempts,
            Duration window,
            Duration blockDuration) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.blockDuration = blockDuration;
    }

    /** Gibt die maximale Anzahl von Versuchen zurück. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /** Ermittelt das Zeitfenster. */
    public Duration window() {
        return window;
    }

    /** Gibt die Sperrdauer zurück. */
    public Duration blockDuration() {
        return blockDuration;
    }
}
