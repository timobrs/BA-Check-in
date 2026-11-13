package de.ba.check.in.application.service;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;

/** Ein Validierungsfehler mit einer für das Sekretariat geeigneten Erklärung. */
public class CalendarImportValidationException extends ApiException {
    public CalendarImportValidationException(String message) {
        super(ApiErrorCode.VALIDATION_FAILED, message);
    }
}
