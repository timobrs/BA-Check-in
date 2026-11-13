package de.ba.check.in.auth.token.repository;

import de.ba.check.in.auth.token.domain.RefreshTokenEntity;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /** Sucht passende Refresh-Token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from RefreshTokenEntity token
            join fetch token.deviceCredential credential
            join fetch credential.device device
            join fetch device.user
            where token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);

    /** Markiert die Token-Familie als wiederverwendet. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
            set token.revokedAt = coalesce(token.revokedAt, :revokedAt),
                token.revocationReason =
                    RefreshTokenRevocationReason.REPLAY_DETECTED
            where token.familyId = :familyId
            """)
    int markFamilyAsReplayed(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt);

    /** Sperrt Token für einen Gerätezugang. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
            set token.revokedAt = :revokedAt,
                token.revocationReason = :reason
            where token.deviceCredential.id = :credentialId
              and token.revokedAt is null
            """)
    int revokeForCredential(
            @Param("credentialId") UUID credentialId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") RefreshTokenRevocationReason reason);
}
