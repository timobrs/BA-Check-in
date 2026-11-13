package de.ba.check.in.users.exception;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

import java.util.UUID;

public class UserNotFoundException extends ApiException {

    /** Erstellt eine Ausnahme. */
    public UserNotFoundException(UUID userId) {
        super(ApiErrorCode.USER_NOT_FOUND, "User not found: %s".formatted(userId));
    }
}
