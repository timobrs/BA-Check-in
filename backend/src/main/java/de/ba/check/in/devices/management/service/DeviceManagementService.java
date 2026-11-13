package de.ba.check.in.devices.management.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.auth.exception.AccessDeniedApiException;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.devices.management.exception.DeviceNotFoundException;
import de.ba.check.in.devices.repository.DeviceRepository;
import de.ba.check.in.users.repository.UserRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class DeviceManagementService {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceRevocationService revocationService;
    private final Clock clock;

    /** Sperrt das Gerät. */
    @Transactional
    public void revoke(UUID deviceId,
            UUID secretaryId,
            @NotBlank(message = "A revoke reason is required")
            @Size(max = 500, message = "Revoke reason must not exceed 500 characters") String reason) {
        UserEntity secretary = userRepository.findById(secretaryId)
                .orElseThrow(() -> new AccessDeniedApiException("Secretary user does not exist"));
        if (secretary.getRole() != UserRole.SECRETARY || secretary.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedApiException("Only an active secretary may revoke devices");
        }
        DeviceEntity device =
                deviceRepository.findByIdForUpdate(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
        if (device.getDeviceStatus() == DeviceStatus.REVOKED) {
            return;
        }
        revocationService.revoke(device,
                secretary,
                clock.instant(),
                reason.trim(),
                RefreshTokenRevocationReason.DEVICE_REVOKED,
                AuditAction.DEVICE_REVOKED_BY_SECRETARY);
    }

    /** Sperrt aktive Gerätezugänge des Nutzers. */
    @Transactional
    public void revokeActiveForUser(UUID userId, UUID secretaryId, String reason) {
        UserEntity secretary = userRepository.findById(secretaryId)
                .orElseThrow(() -> new AccessDeniedApiException("Secretary user does not exist"));
        if (secretary.getRole() != UserRole.SECRETARY || secretary.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedApiException("Only an active secretary may revoke devices");
        }
        for (DeviceEntity device : deviceRepository.findActiveByUserIdForUpdate(userId)) {
            revocationService.revoke(device, secretary, clock.instant(), reason,
                    RefreshTokenRevocationReason.DEVICE_REVOKED, AuditAction.DEVICE_REVOKED_BY_SECRETARY);
        }
    }
}
