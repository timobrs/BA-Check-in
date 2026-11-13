package de.ba.check.in.auth.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public abstract class DeviceAuthenticationException extends ApiException {

    /** Erstellt eine Ausnahme. */
    protected DeviceAuthenticationException(String internalMessage) {
        super(ApiErrorCode.DEVICE_AUTHENTICATION_FAILED, internalMessage);
    }
}
