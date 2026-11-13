package de.ba.check.in.auth.exception;

public class InvalidChallengeException extends DeviceAuthenticationException {

    /** Erstellt eine Ausnahme. */
    public InvalidChallengeException() {
        super("Authentication challenge is invalid or expired");
    }
}
