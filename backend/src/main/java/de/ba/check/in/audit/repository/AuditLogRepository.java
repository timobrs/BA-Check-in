package de.ba.check.in.audit.repository;

import de.ba.check.in.audit.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    /** Zählt passende Audit-Einträge. */
    long countByAction(String action);

    /** Zählt passende Audit-Einträge. */
    long countByActionAndTargetId(String action, UUID targetId);
}
