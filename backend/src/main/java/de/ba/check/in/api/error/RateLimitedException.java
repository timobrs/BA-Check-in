package de.ba.check.in.api.error;

import java.time.Instant;

public class RateLimitedException extends ApiException {
    private final Instant retryAt;

    /** Erstellt eine Ausnahme. */
    public RateLimitedException(Instant retryAt) {
        super(ApiErrorCode.RATE_LIMITED);
        this.retryAt = retryAt;
    }

    /** Gibt den Zeitpunkt für einen neuen Versuch zurück. */
    public Instant getRetryAt() {
        return retryAt;
    }
}
