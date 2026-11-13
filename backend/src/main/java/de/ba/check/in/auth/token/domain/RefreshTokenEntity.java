package de.ba.check.in.auth.token.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity extends CreatedOnlyEntity {

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_credential_id", nullable = false)
    private DeviceCredentialEntity deviceCredential;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] tokenHash;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "family_expires_at", nullable = false)
    private Instant familyExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason", length = 50)
    private RefreshTokenRevocationReason revocationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_token_id")
    private RefreshTokenEntity parentToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshTokenEntity replacedByToken;

    /** Erstellt das erste Refresh-Token. */
    public static RefreshTokenEntity initial(
            UUID familyId,
            DeviceCredentialEntity credential,
            byte[] tokenHash,
            Instant expiresAt,
            Instant familyExpiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.familyId = familyId;
        entity.deviceCredential = credential;
        entity.tokenHash = tokenHash.clone();
        entity.sessionVersion = credential.getSessionVersion();
        entity.expiresAt = expiresAt;
        entity.familyExpiresAt = familyExpiresAt;
        return entity;
    }

    /** Erstellt ein Ersatz-Refresh-Token. */
    public static RefreshTokenEntity replacement(
            RefreshTokenEntity parent,
            byte[] tokenHash,
            Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.familyId = parent.familyId;
        entity.deviceCredential = parent.deviceCredential;
        entity.tokenHash = tokenHash.clone();
        entity.sessionVersion = parent.sessionVersion;
        entity.expiresAt = expiresAt;
        entity.familyExpiresAt = parent.familyExpiresAt;
        entity.parentToken = parent;
        return entity;
    }

    /** Prüft, ob der Eintrag abgelaufen ist. */
    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now) || !familyExpiresAt.isAfter(now);
    }

    /** Prüft, ob das Token erneuert wurde. */
    public boolean isRotated() {
        return consumedAt != null
                || replacedByToken != null
                || revocationReason == RefreshTokenRevocationReason.ROTATED;
    }

    /** Prüft, ob der Eintrag gesperrt ist. */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Verwendet das Token für die Erneuerung. */
    public void consumeForRotation(Instant now) {
        consumedAt = now;
        revokedAt = now;
        revocationReason = RefreshTokenRevocationReason.ROTATED;
    }

    /** Verknüpft das Ersatz-Token. */
    public void linkReplacement(RefreshTokenEntity replacement) {
        replacedByToken = replacement;
    }
}
