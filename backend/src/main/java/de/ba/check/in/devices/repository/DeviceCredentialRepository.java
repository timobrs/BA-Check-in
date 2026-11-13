package de.ba.check.in.devices.repository;

import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.users.domain.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, UUID> {

    /** Prüft auf einen aktiven Gerätezugang für die Rolle. */
    @Query("""
            select count(credential) > 0
            from DeviceCredentialEntity credential
            join credential.device device
            join device.user user
            where user.role = :role
            and user.status = de.ba.check.in.users.domain.UserStatus.ACTIVE
            and device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
            and credential.active = true
            """)
    boolean hasActiveCredentialForRole(@Param("role") UserRole role);

    /** Prüft, ob der Gerätezugang aktiv ist. */
    @Query("""
            select count(credential) > 0
            from DeviceCredentialEntity credential
            join credential.device device
            join device.user user
            where credential.id = :credentialId
            and credential.sessionVersion = :sessionVersion
            and credential.active = true
            and device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
            and user.status = de.ba.check.in.users.domain.UserStatus.ACTIVE
            """)
    boolean isCredentialActive(
            @Param("credentialId") UUID credentialId,
            @Param("sessionVersion") long sessionVersion);

    /** Prüft, ob der Gerätezugang aktiv ist. */
    @Query("""
            select count(credential) > 0
            from DeviceCredentialEntity credential
            join credential.device device
            join device.user user
            where credential.id = :credentialId
            and credential.sessionVersion = :sessionVersion
            and device.id = :deviceId
            and user.id = :userId
            and user.role = :role
            and credential.active = true
            and device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
            and user.status = de.ba.check.in.users.domain.UserStatus.ACTIVE
            """)
    boolean isCredentialActive(
            @Param("credentialId") UUID credentialId,
            @Param("sessionVersion") long sessionVersion,
            @Param("userId") UUID userId,
            @Param("deviceId") UUID deviceId,
            @Param("role") UserRole role);

    /** Sucht passende Gerätezugänge. */
    @Query("""
            select credential
            from DeviceCredentialEntity credential
            join fetch credential.device device
            join fetch device.user user
            where credential.id =:credentialId
            and credential.active = true
            and device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
            and user.status = de.ba.check.in.users.domain.UserStatus.ACTIVE
            """)
    Optional<DeviceCredentialEntity> findActiveCredential(@Param("credentialId") UUID credentialId);

    /** Sucht passende Gerätezugänge. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from DeviceCredentialEntity credential
            join fetch credential.device device
            join fetch device.user
            where credential.id = :credentialId
            """)
    Optional<DeviceCredentialEntity> findByIdForUpdate(@Param("credentialId") UUID credentialId);

    /** Sucht passende Gerätezugänge. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from DeviceCredentialEntity credential
            join fetch credential.device device
            where device.user.id = :userId
              and credential.active = true
            """)
    List<DeviceCredentialEntity> findActiveByUserIdForUpdate(@Param("userId") UUID userId);

    /** Sucht passende Gerätezugänge. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from DeviceCredentialEntity credential
            join fetch credential.device device
            join fetch device.user
            where device.id = :deviceId
            """)
    List<DeviceCredentialEntity> findByDeviceIdForUpdate(@Param("deviceId") UUID deviceId);

    /** Sucht passende Gerätezugänge. */
    Optional<DeviceCredentialEntity> findByPublicKeyFingerprint(byte[] publicKeyFingerprint);
}
