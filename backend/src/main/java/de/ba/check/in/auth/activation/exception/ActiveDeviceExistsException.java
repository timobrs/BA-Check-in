package de.ba.check.in.auth.activation.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class ActiveDeviceExistsException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public ActiveDeviceExistsException() {
        super(ApiErrorCode.ACTIVE_DEVICE_EXISTS);
    }
}
