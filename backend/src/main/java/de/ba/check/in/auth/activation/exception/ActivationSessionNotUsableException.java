package de.ba.check.in.auth.activation.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class ActivationSessionNotUsableException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public ActivationSessionNotUsableException() {
        super(ApiErrorCode.ACTIVATION_SESSION_NOT_USABLE);
    }
}
