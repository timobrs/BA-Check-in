package de.ba.check.in.auth.token.domain;

public enum RefreshTokenRevocationReason {
    ROTATED,
    REPLAY_DETECTED,
    LOGOUT,
    DEVICE_REVOKED,
    DEVICE_REPLACED,
    NEW_LOGIN
}
