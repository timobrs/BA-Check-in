package de.ba.check.in.devices.repository;

import de.ba.check.in.devices.domain.DeviceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    /** Sucht passende Geräte. */
    List<DeviceEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Lädt aktive Studierendengeräte für die Sekretariatsübersicht. */
    @Query("""
            select device
            from DeviceEntity device join fetch device.user owner
            where device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
              and owner.role = de.ba.check.in.users.domain.UserRole.STUDENT
            """)
    List<DeviceEntity> findActiveStudentDevices();

    /** Sucht passende Geräte. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select device
            from DeviceEntity device
            where device.id = :deviceId
            """)
    Optional<DeviceEntity> findByIdForUpdate(@Param("deviceId") UUID deviceId);

    /** Sucht passende Geräte. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select device
            from DeviceEntity device
            where device.user.id = :userId
              and device.deviceStatus = de.ba.check.in.devices.domain.DeviceStatus.ACTIVE
            """)
    List<DeviceEntity> findActiveByUserIdForUpdate(@Param("userId") UUID userId);
}
