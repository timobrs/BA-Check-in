package de.ba.check.in.auth.session.repository;

import de.ba.check.in.auth.session.domain.AuthChallengeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthChallengeRepository extends JpaRepository<AuthChallengeEntity, UUID> {

    /** Sucht passende Challenges. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from AuthChallengeEntity challenge
            join fetch challenge.deviceCredential credential
            join fetch credential.device device
            join fetch device.user
            where challenge.id = :challengeId
            """)
    Optional<AuthChallengeEntity> findByIdForUpdate(@Param("challengeId") UUID challengeId);

    /** Markiert offene Challenges als verwendet. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update AuthChallengeEntity challenge
            set challenge.consumedAt = :consumedAt
            where challenge.deviceCredential.id = :credentialId
              and challenge.consumedAt is null
            """)
    int consumeOpenChallenges(
            @Param("credentialId") UUID credentialId,
            @Param("consumedAt") Instant consumedAt);
}
