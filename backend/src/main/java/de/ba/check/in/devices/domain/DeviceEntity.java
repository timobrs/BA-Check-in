package de.ba.check.in.devices.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceEntity extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "device_name", nullable = false, length = 255)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DeviceStatus deviceStatus = DeviceStatus.PENDING_APPROVAL;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "os_version", length = 100)
    private String osVersion;

    @Column(name = "app_version", length = 100)
    private String appVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserEntity approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private UserEntity revokedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    private String revokeReason;

    /** Aktiviert das Gerät. */
    public static DeviceEntity activate(
            UserEntity user,
            String deviceName,
            Platform platform,
            String manufacturer,
            String model,
            String osVersion,
            String appVersion,
            UserEntity approvedBy,
            Instant now) {
        DeviceEntity entity = new DeviceEntity();
        entity.user = user;
        entity.deviceName = deviceName;
        entity.platform = platform;
        entity.manufacturer = manufacturer;
        entity.model = model;
        entity.osVersion = osVersion;
        entity.appVersion = appVersion;
        entity.deviceStatus = DeviceStatus.ACTIVE;
        entity.approvedBy = approvedBy;
        entity.approvedAt = now;
        return entity;
    }

    /** Sperrt das Gerät. */
    public void revoke(UserEntity actor, Instant now, String reason) {
        if (deviceStatus == DeviceStatus.REVOKED) {
            return;
        }
        deviceStatus = DeviceStatus.REVOKED;
        revokedBy = actor;
        revokedAt = now;
        revokeReason = reason;
    }

    /** Ersetzt die Gerätebindung. */
    public void replace(UserEntity actor, Instant now) {
        deviceStatus = DeviceStatus.REPLACED;
        revokedBy = actor;
        revokedAt = now;
        revokeReason = "Replaced by a newly activated device";
    }
}
