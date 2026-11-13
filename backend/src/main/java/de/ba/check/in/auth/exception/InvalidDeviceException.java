package de.ba.check.in.auth.exception;

public class InvalidDeviceException extends DeviceAuthenticationException {

    /** Erstellt eine Ausnahme. */
    public InvalidDeviceException() {
        super("Device credential is unknown or not active");
    }
}
