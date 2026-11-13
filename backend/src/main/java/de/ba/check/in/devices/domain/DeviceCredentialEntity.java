package de.ba.check.in.devices.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;

@Entity
@Table(name = "device_credentials")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceCredentialEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "public_key_fingerprint", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] publicKeyFingerprint;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm = "ES256";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Erstellt einen Gerätezugang. */
    public static DeviceCredentialEntity create(
            DeviceEntity device,
            String publicKey,
            byte[] publicKeyFingerprint,
            String algorithm) {
        DeviceCredentialEntity entity = new DeviceCredentialEntity();
        entity.device = device;
        entity.publicKey = publicKey;
        entity.publicKeyFingerprint = Arrays.copyOf(publicKeyFingerprint, publicKeyFingerprint.length);
        entity.algorithm = algorithm;
        entity.active = true;
        return entity;
    }

    /** Erfasst die Verwendung. */
    public void recordUse(Instant now) {
        lastUsedAt = now;
    }

    /** Beendet die Gerätesitzungen. */
    public void endSessions() {
        sessionVersion++;
    }

    /** Deaktiviert den Gerätezugang. */
    public void deactivate() {
        active = false;
        sessionVersion++;
    }
}
