package de.ba.check.in.auth.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class AccessDeniedApiException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public AccessDeniedApiException(String internalMessage) {
        super(ApiErrorCode.ACCESS_DENIED, internalMessage);
    }
}
