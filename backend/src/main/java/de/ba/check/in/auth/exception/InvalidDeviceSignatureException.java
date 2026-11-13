package de.ba.check.in.auth.exception;

public class InvalidDeviceSignatureException extends DeviceAuthenticationException {

    /** Erstellt eine Ausnahme. */
    public InvalidDeviceSignatureException() {
        super("Device signature verification failed");
    }
}
