package de.ba.check.in.auth.session.service;

import de.ba.check.in.auth.exception.InvalidChallengeException;
import de.ba.check.in.auth.exception.InvalidDeviceException;
import de.ba.check.in.auth.session.domain.AuthChallengeEntity;
import de.ba.check.in.auth.session.dto.OpenDeviceSessionRequest;
import de.ba.check.in.auth.session.model.ClaimedDeviceChallenge;
import de.ba.check.in.auth.session.repository.AuthChallengeRepository;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.users.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requireNotNegative;

@Service
public class DeviceAuthenticationAttemptService {
    private final AuthChallengeRepository challengeRepository;
    private final Clock clock;
    private final Duration allowedClockSkew;

    /** Erstellt den Dienst. */
    public DeviceAuthenticationAttemptService(AuthChallengeRepository challengeRepository,
            Clock clock,
            @Value("${app.security.device-session.allowed-clock-skew:PT1M}") Duration allowedClockSkew) {
        this.challengeRepository = challengeRepository;
        this.clock = clock;
        this.allowedClockSkew = requireNotNegative(allowedClockSkew, "app.security.device-session.allowed-clock-skew");
    }

    /** Prüft und verwendet die Login-Challenge. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimedDeviceChallenge claim(OpenDeviceSessionRequest request) {
        Instant now = clock.instant();
        AuthChallengeEntity challenge = challengeRepository.findByIdForUpdate(request.authSessionId())
                .orElseThrow(InvalidChallengeException::new);
        DeviceCredentialEntity credential = challenge.getDeviceCredential();
        if (!credential.getId().equals(request.deviceCredentialId()) || !challenge.isUsableAt(now)) {
            throw new InvalidChallengeException();
        }
        if (!isActive(credential)) {
            throw new InvalidDeviceException();
        }
        Duration clockDifference = Duration.between(Instant.ofEpochMilli(request.timestampEpochMillis()), now).abs();
        if (clockDifference.compareTo(allowedClockSkew) > 0) {
            throw new InvalidChallengeException();
        }
        challenge.consume(now);
        return new ClaimedDeviceChallenge(credential.getId(),
                challenge.getId(),
                credential.getPublicKey(),
                challenge.getChallenge(),
                request.timestampEpochMillis());
    }

    /** Prüft den Aktivstatus. */
    private boolean isActive(DeviceCredentialEntity credential) {
        return credential.isActive()
                && credential.getDevice().getDeviceStatus() == DeviceStatus.ACTIVE
                && credential.getDevice().getUser().getStatus() == UserStatus.ACTIVE;
    }
}
