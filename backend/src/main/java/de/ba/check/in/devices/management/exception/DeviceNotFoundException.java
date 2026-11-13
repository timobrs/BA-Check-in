package de.ba.check.in.devices.management.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

import java.util.UUID;

public class DeviceNotFoundException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public DeviceNotFoundException(UUID deviceId) {
        super(ApiErrorCode.DEVICE_NOT_FOUND, "Device not found: %s".formatted(deviceId));
    }
}
