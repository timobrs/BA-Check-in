package de.ba.check.in.audit.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.domain.AuditLogEntity;
import de.ba.check.in.audit.repository.AuditLogRepository;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.users.domain.UserEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SecurityAuditService {
    private final AuditLogRepository auditLogRepository;

    /** Erstellt den Dienst. */
    public SecurityAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Schreibt einen Audit-Eintrag. */
    public void record(
            AuditAction action,
            UserEntity actorUser,
            DeviceEntity actorDevice,
            String targetType,
            UUID targetId,
            String details) {
        auditLogRepository.save(AuditLogEntity.create(
                actorUser,
                actorDevice,
                action,
                targetType,
                targetId,
                details));
    }
}
