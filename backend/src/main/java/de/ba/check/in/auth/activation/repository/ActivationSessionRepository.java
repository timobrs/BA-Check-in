package de.ba.check.in.auth.activation.repository;

import de.ba.check.in.auth.activation.domain.ActivationSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActivationSessionRepository extends JpaRepository<ActivationSessionEntity, UUID> {

    /** Lädt die Daten für die Prüfung. */
    @Query("""
            select session
            from ActivationSessionEntity session
            join fetch session.activationCode code
            join fetch code.user
            where session.id = :sessionId
            """)
    Optional<ActivationSessionEntity> findForVerification(@Param("sessionId") UUID sessionId);

    /** Sucht passende Aktivierungssitzungen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from ActivationSessionEntity session
            join fetch session.activationCode code
            join fetch code.user
            join fetch code.createdBy
            left join fetch session.resultDeviceCredential resultCredential
            left join fetch resultCredential.device
            where session.id = :sessionId
            """)
    Optional<ActivationSessionEntity> findByIdForUpdate(@Param("sessionId") UUID sessionId);

    /** Macht offene Sitzungen ungültig. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update ActivationSessionEntity session
            set session.invalidatedAt = :invalidatedAt
            where session.activationCode.id = :activationCodeId
              and session.consumedAt is null
              and session.invalidatedAt is null
            """)
    int invalidateOpenSessions(
            @Param("activationCodeId") UUID activationCodeId,
            @Param("invalidatedAt") Instant invalidatedAt);
}
