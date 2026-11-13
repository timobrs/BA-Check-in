package de.ba.check.in.api.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "Validierung fehlgeschlagen",
            "Die übermittelten Daten sind ungültig."
    ),
    MALFORMED_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Fehlerhafte Anfrage",
            "Die Anfrage ist fehlerhaft oder konnte nicht verarbeitet werden."
    ),
    DEVICE_AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "Authentifizierung fehlgeschlagen",
            "Die Geräte-Authentifizierung ist fehlgeschlagen."
    ),
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "Zugriff verweigert",
            "Sie haben keine Berechtigung für diese Aktion."
    ),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "Sitzung abgelaufen",
            "Der Refresh-Token ist ungültig oder abgelaufen. Bitte melden Sie sich erneut an."
    ),
    ACTIVATION_CODE_NOT_USABLE(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Freischaltung fehlgeschlagen",
            "Der Freischaltcode ist ungültig oder nicht mehr verwendbar."
    ),
    ACTIVATION_SESSION_NOT_USABLE(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Freischaltung fehlgeschlagen",
            "Die Freischaltsitzung ist ungültig oder abgelaufen."
    ),
    ACTIVATION_MODE_NOT_ALLOWED(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Freischaltung nicht erlaubt",
            "Dieser Freischaltmodus ist für Ihren Benutzer nicht zulässig."
    ),
    ACTIVE_DEVICE_EXISTS(
            HttpStatus.CONFLICT,
            "Gerätekonflikt",
            "Es ist bereits ein aktives Gerät für diesen Benutzer registriert."
    ),
    DEVICE_KEY_REJECTED(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Geräteschlüssel abgelehnt",
            "Der kryptografische Nachweis des Geräteschlüssels ist ungültig."
    ),
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Benutzer nicht gefunden",
            "Der angeforderte Benutzer existiert nicht."
    ),
    DEVICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Gerät nicht gefunden",
            "Das angeforderte Gerät existiert nicht."
    ),
    RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Zu viele Anfragen",
            "Bitte versuchen Sie es zu einem späteren Zeitpunkt erneut."
    ),
    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Interner Serverfehler",
            "Die Anfrage konnte aufgrund eines unerwarteten Fehlers nicht verarbeitet werden."
    );

    private final HttpStatus status;
    private final String title;
    private final String detail;

    /** Erstellt eine neue Instanz. */
    ApiErrorCode(HttpStatus status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    /** Ermittelt den Status. */
    public HttpStatus status() {
        return status;
    }

    /** Gibt den Titel zurück. */
    public String title() {
        return title;
    }

    /** Gibt die Fehlerbeschreibung zurück. */
    public String detail() {
        return detail;
    }
}
