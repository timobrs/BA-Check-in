package de.ba.check.in.auth.session.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "auth_challenges")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthChallengeEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_credential_id", nullable = false)
    private DeviceCredentialEntity deviceCredential;

    @Column(name = "challenge", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] challenge;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    /** Erstellt eine Challenge. */
    public static AuthChallengeEntity create(
            DeviceCredentialEntity credential,
            byte[] challenge,
            Instant expiresAt) {
        AuthChallengeEntity entity = new AuthChallengeEntity();
        entity.deviceCredential = credential;
        entity.challenge = challenge.clone();
        entity.expiresAt = expiresAt;
        return entity;
    }

    /** Prüft die Nutzbarkeit zum angegebenen Zeitpunkt. */
    public boolean isUsableAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    /** Markiert den Wert als verwendet. */
    public void consume(Instant now) {
        consumedAt = now;
    }
}
