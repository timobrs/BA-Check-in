package de.ba.check.in.devices.management.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.session.repository.AuthChallengeRepository;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.auth.token.repository.RefreshTokenRepository;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeviceRevocationService {
    private final DeviceCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthChallengeRepository challengeRepository;
    private final SecurityAuditService auditService;

    /** Sperrt das Gerät. */
    public void revoke(DeviceEntity device,
            UserEntity actor,
            Instant now,
            String reason,
            RefreshTokenRevocationReason tokenReason,
            AuditAction auditAction) {
        for (DeviceCredentialEntity credential : credentialRepository.findByDeviceIdForUpdate(device.getId())) {
            if (credential.isActive()) {
                credential.deactivate();
            }
            refreshTokenRepository.revokeForCredential(credential.getId(), now, tokenReason);
            challengeRepository.consumeOpenChallenges(credential.getId(), now);
        }
        device.revoke(actor, now, reason);
        auditService.record(auditAction,
                actor,
                auditAction == AuditAction.DEVICE_UNLINKED_BY_OWNER ? device : null,
                "DEVICE",
                device.getId(),
                "reason=%s".formatted(reason));
    }
}
