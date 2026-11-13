package de.ba.check.in.auth.devicekey.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

public class DeviceKeyRejectedException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public DeviceKeyRejectedException(String internalMessage) {
        super(ApiErrorCode.DEVICE_KEY_REJECTED, internalMessage);
    }
}
