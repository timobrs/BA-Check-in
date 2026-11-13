package de.ba.check.in.auth.activation.repository;

import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActivationCodeRepository extends JpaRepository<ActivationCodeEntity, UUID> {

    /** Sucht passende Aktivierungscodes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select code
            from ActivationCodeEntity code
            join fetch code.user
            join fetch code.createdBy
            where code.id = :codeId
            """)
    Optional<ActivationCodeEntity> findByIdForUpdate(@Param("codeId") UUID codeId);

    /** Sucht passende Aktivierungscodes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select code
            from ActivationCodeEntity code
            join fetch code.user
            join fetch code.createdBy
            where code.codeHash = :codeHash
            """)
    Optional<ActivationCodeEntity> findByCodeHashForUpdate(@Param("codeHash") byte[] codeHash);

    /** Sperrt offene Aktivierungscodes. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update ActivationCodeEntity code
            set code.revokedAt = :revokedAt
            where code.user.id = :userId
              and code.consumedAt is null
              and code.revokedAt is null
            """)
    int revokeOpenCodes(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
