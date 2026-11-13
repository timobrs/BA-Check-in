package de.ba.check.in.auth.session.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.exception.InvalidDeviceException;
import de.ba.check.in.auth.session.dto.TokenPairResponse;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.auth.token.model.IssuedAccessToken;
import de.ba.check.in.auth.token.model.IssuedRefreshToken;
import de.ba.check.in.auth.token.repository.RefreshTokenRepository;
import de.ba.check.in.auth.token.service.JwtService;
import de.ba.check.in.auth.token.service.RefreshTokenService;
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
public class DeviceSessionIssuer {
    private final DeviceCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityAuditService auditService;
    private final Clock clock;

    /** Erstellt eine Gerätesitzung mit Tokens. */
    @Transactional
    public TokenPairResponse issue(UUID credentialId) {
        Instant now = clock.instant();
        DeviceCredentialEntity credential =
                credentialRepository.findByIdForUpdate(credentialId).orElseThrow(InvalidDeviceException::new);
        if (!isActive(credential)) {
            throw new InvalidDeviceException();
        }
        refreshTokenRepository.revokeForCredential(credentialId, now, RefreshTokenRevocationReason.NEW_LOGIN);
        credential.endSessions();
        credential.recordUse(now);
        IssuedAccessToken accessToken = jwtService.createAccessToken(credential);
        IssuedRefreshToken refreshToken = refreshTokenService.createInitialToken(credential);
        auditService.record(AuditAction.DEVICE_LOGIN_SUCCEEDED,
                credential.getDevice().getUser(),
                credential.getDevice(),
                "DEVICE_CREDENTIAL",
                credential.getId(),
                "sessionVersion=%d".formatted(credential.getSessionVersion()));
        return new TokenPairResponse(accessToken.accessToken(),
                refreshToken.refreshToken(),
                accessToken.expiresAt(),
                refreshToken.expiresAt());
    }

    /** Prüft den Aktivstatus. */
    private boolean isActive(DeviceCredentialEntity credential) {
        return credential.isActive()
                && credential.getDevice().getDeviceStatus() == DeviceStatus.ACTIVE
                && credential.getDevice().getUser().getStatus() == UserStatus.ACTIVE;
    }
}
