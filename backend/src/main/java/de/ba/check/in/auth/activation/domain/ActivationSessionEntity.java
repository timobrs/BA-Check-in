package de.ba.check.in.auth.activation.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "activation_sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivationSessionEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activation_code_id", nullable = false)
    private ActivationCodeEntity activationCode;

    @Column(name = "challenge", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] challenge;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_device_credential_id")
    private DeviceCredentialEntity resultDeviceCredential;

    /** Erstellt eine Aktivierungssitzung. */
    public static ActivationSessionEntity create(
            ActivationCodeEntity activationCode,
            byte[] challenge,
            Instant expiresAt) {
        ActivationSessionEntity entity = new ActivationSessionEntity();
        entity.activationCode = activationCode;
        entity.challenge = challenge.clone();
        entity.expiresAt = expiresAt;
        return entity;
    }

    /** Prüft die Nutzbarkeit zum angegebenen Zeitpunkt. */
    public boolean isUsableAt(Instant now) {
        return consumedAt == null && invalidatedAt == null && expiresAt.isAfter(now);
    }

    /** Schließt die Aktivierungssitzung ab. */
    public void complete(DeviceCredentialEntity credential, Instant now) {
        resultDeviceCredential = credential;
        consumedAt = now;
    }
}
