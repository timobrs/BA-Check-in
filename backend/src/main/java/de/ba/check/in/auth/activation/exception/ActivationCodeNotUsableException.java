package de.ba.check.in.auth.activation.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class ActivationCodeNotUsableException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public ActivationCodeNotUsableException() {
        super(ApiErrorCode.ACTIVATION_CODE_NOT_USABLE);
    }
}
