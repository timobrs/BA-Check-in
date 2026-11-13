package de.ba.check.in.audit.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserEntity actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_device_id")
    private DeviceEntity actorDevice;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** Erstellt einen Audit-Eintrag. */
    public static AuditLogEntity create(
            UserEntity actorUser,
            DeviceEntity actorDevice,
            AuditAction action,
            String targetType,
            UUID targetId,
            String details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.actorUser = actorUser;
        entity.actorDevice = actorDevice;
        entity.action = action.name();
        entity.targetType = targetType;
        entity.targetId = targetId;
        entity.details = details;
        return entity;
    }
}
