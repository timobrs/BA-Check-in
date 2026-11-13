package de.ba.check.in.api.error;

public class ApiException extends RuntimeException {
    private final ApiErrorCode errorCode;

    /** Erstellt eine Ausnahme. */
    public ApiException(ApiErrorCode errorCode) {
        this(errorCode, errorCode.name());
    }

    /** Erstellt eine Ausnahme. */
    public ApiException(ApiErrorCode errorCode, String internalMessage) {
        super(internalMessage);
        this.errorCode = errorCode;
    }

    /** Gibt den Fehlercode zurück. */
    public ApiErrorCode getErrorCode() {
        return errorCode;
    }
}
