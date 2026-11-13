package de.ba.check.in.auth.token.model;

public record RefreshRotationResult(
        RefreshRotationStatus status,
        IssuedAccessToken accessToken,
        IssuedRefreshToken refreshToken
) {

    /** Erstellt ein Ergebnis für ein erneuertes Token. */
    public static RefreshRotationResult rotated(
            IssuedAccessToken accessToken,
            IssuedRefreshToken refreshToken) {
        return new RefreshRotationResult(
                RefreshRotationStatus.ROTATED,
                accessToken,
                refreshToken);
    }

    /** Erstellt das Ergebnis für ein ungültiges Token. */
    public static RefreshRotationResult invalid() {
        return new RefreshRotationResult(RefreshRotationStatus.INVALID, null, null);
    }

    /** Erstellt ein Ergebnis für eine Wiederverwendung. */
    public static RefreshRotationResult replayDetected() {
        return new RefreshRotationResult(
                RefreshRotationStatus.REPLAY_DETECTED,
                null,
                null);
    }
}
