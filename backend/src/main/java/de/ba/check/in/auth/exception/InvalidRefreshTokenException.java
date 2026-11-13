package de.ba.check.in.auth.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class InvalidRefreshTokenException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public InvalidRefreshTokenException() {
        super(ApiErrorCode.INVALID_REFRESH_TOKEN);
    }
}
