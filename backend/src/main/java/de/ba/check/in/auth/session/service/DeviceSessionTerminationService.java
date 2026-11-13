package de.ba.check.in.auth.session.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.exception.InvalidDeviceException;
import de.ba.check.in.auth.session.repository.AuthChallengeRepository;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.auth.token.repository.RefreshTokenRepository;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceSessionTerminationService {
    private final DeviceCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthChallengeRepository challengeRepository;
    private final SecurityAuditService auditService;
    private final Clock clock;

    /** Beendet die Sitzung. */
    @Transactional
    public void logout(UUID credentialId, long authenticatedSessionVersion) {
        DeviceCredentialEntity credential =
                credentialRepository.findByIdForUpdate(credentialId).orElseThrow(InvalidDeviceException::new);
        if (!isActive(credential) || credential.getSessionVersion() != authenticatedSessionVersion) {
            throw new InvalidDeviceException();
        }
        Instant now = clock.instant();
        refreshTokenRepository.revokeForCredential(credentialId, now, RefreshTokenRevocationReason.LOGOUT);
        challengeRepository.consumeOpenChallenges(credentialId, now);
        credential.endSessions();
        auditService.record(AuditAction.DEVICE_SESSION_LOGGED_OUT,
                credential.getDevice().getUser(),
                credential.getDevice(),
                "DEVICE_CREDENTIAL",
                credentialId,
                "sessionVersion=%d".formatted(credential.getSessionVersion()));
    }

    /** Prüft den Aktivstatus. */
    private boolean isActive(DeviceCredentialEntity credential) {
        return credential.isActive()
                && credential.getDevice().getDeviceStatus() == DeviceStatus.ACTIVE
                && credential.getDevice().getUser().getStatus() == UserStatus.ACTIVE;
    }
}
