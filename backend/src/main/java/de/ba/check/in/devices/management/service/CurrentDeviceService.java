package de.ba.check.in.devices.management.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.auth.exception.InvalidDeviceException;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.devices.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentDeviceService {
    private static final String SELF_REVOKE_REASON = "Removed by device owner";

    private final DeviceCredentialRepository credentialRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceRevocationService revocationService;
    private final Clock clock;

    /** Löst das Gerät vom Konto. */
    @Transactional
    public void unlink(UUID authenticatedUserId, UUID credentialId, long authenticatedSessionVersion) {
        DeviceCredentialEntity preliminary =
                credentialRepository.findActiveCredential(credentialId).orElseThrow(InvalidDeviceException::new);
        UUID deviceId = preliminary.getDevice().getId();
        DeviceEntity device = deviceRepository.findByIdForUpdate(deviceId).orElseThrow(InvalidDeviceException::new);
        DeviceCredentialEntity credential =
                credentialRepository.findByIdForUpdate(credentialId).orElseThrow(InvalidDeviceException::new);
        if (!credential.getDevice().getId().equals(deviceId)
                || !device.getUser().getId().equals(authenticatedUserId)
                || device.getDeviceStatus() != DeviceStatus.ACTIVE
                || !credential.isActive()
                || credential.getSessionVersion() != authenticatedSessionVersion) {
            throw new InvalidDeviceException();
        }
        Instant now = clock.instant();
        revocationService.revoke(device,
                device.getUser(),
                now,
                SELF_REVOKE_REASON,
                RefreshTokenRevocationReason.DEVICE_REVOKED,
                AuditAction.DEVICE_UNLINKED_BY_OWNER);
    }
}
