package de.ba.check.in.auth.activation.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class ActivationModeNotAllowedException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public ActivationModeNotAllowedException(String internalMessage) {
        super(ApiErrorCode.ACTIVATION_MODE_NOT_ALLOWED, internalMessage);
    }
}
