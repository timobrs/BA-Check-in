package de.ba.check.in.auth.session.service;

import de.ba.check.in.auth.crypto.DeviceSignatureService;
import de.ba.check.in.auth.crypto.Sha256Hasher;
import de.ba.check.in.auth.exception.InvalidDeviceException;
import de.ba.check.in.auth.exception.InvalidDeviceSignatureException;
import de.ba.check.in.auth.exception.InvalidRefreshTokenException;
import de.ba.check.in.auth.session.dto.CreateDeviceChallengeResponse;
import de.ba.check.in.auth.session.dto.OpenDeviceSessionRequest;
import de.ba.check.in.auth.session.dto.RefreshDeviceSessionRequest;
import de.ba.check.in.auth.session.dto.TokenPairResponse;
import de.ba.check.in.auth.session.domain.AuthChallengeEntity;
import de.ba.check.in.auth.session.model.ClaimedDeviceChallenge;
import de.ba.check.in.auth.session.repository.AuthChallengeRepository;
import de.ba.check.in.auth.token.model.RefreshRotationResult;
import de.ba.check.in.auth.token.model.RefreshRotationStatus;
import de.ba.check.in.auth.token.service.RefreshTokenService;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requireNotNegative;
import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
public class DeviceSessionService {
    private final DeviceCredentialRepository credentialRepository;
    private final AuthChallengeRepository challengeRepository;
    private final DeviceAuthenticationAttemptService authenticationAttemptService;
    private final DeviceSignatureService signatureService;
    private final DeviceSessionIssuer sessionIssuer;
    private final DeviceSessionTerminationService terminationService;
    private final RefreshTokenService refreshTokenService;
    private final Sha256Hasher sha256Hasher;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Duration challengeTtl;
    private final Duration allowedClockSkew;

    /** Erstellt den Dienst. */
    public DeviceSessionService(DeviceCredentialRepository credentialRepository,
            AuthChallengeRepository challengeRepository,
            DeviceAuthenticationAttemptService authenticationAttemptService,
            DeviceSignatureService signatureService,
            DeviceSessionIssuer sessionIssuer,
            DeviceSessionTerminationService terminationService,
            RefreshTokenService refreshTokenService,
            Sha256Hasher sha256Hasher,
            Clock clock,
            SecureRandom secureRandom,
            @Value("${app.security.device-session.challenge-ttl:PT2M}") Duration challengeTtl,
            @Value("${app.security.device-session.allowed-clock-skew:PT1M}") Duration allowedClockSkew) {
        this.credentialRepository = credentialRepository;
        this.challengeRepository = challengeRepository;
        this.authenticationAttemptService = authenticationAttemptService;
        this.signatureService = signatureService;
        this.sessionIssuer = sessionIssuer;
        this.terminationService = terminationService;
        this.refreshTokenService = refreshTokenService;
        this.sha256Hasher = sha256Hasher;
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.challengeTtl = requirePositive(challengeTtl, "app.security.device-session.challenge-ttl");
        this.allowedClockSkew = requireNotNegative(allowedClockSkew, "app.security.device-session.allowed-clock-skew");
    }

    /** Erstellt eine Challenge. */
    @Transactional
    public CreateDeviceChallengeResponse createChallenge(@NotNull UUID credentialId) {
        DeviceCredentialEntity credential =
                credentialRepository.findByIdForUpdate(credentialId).orElseThrow(InvalidDeviceException::new);
        if (!isActive(credential)) {
            throw new InvalidDeviceException();
        }
        Instant now = clock.instant();
        challengeRepository.consumeOpenChallenges(credentialId, now);
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        Instant expiresAt = now.plus(challengeTtl);
        AuthChallengeEntity challenge = AuthChallengeEntity.create(credential, challengeBytes, expiresAt);
        challengeRepository.save(challenge);
        return new CreateDeviceChallengeResponse(challenge.getId(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes),
                expiresAt);
    }

    /** Öffnet eine Sitzung. */
    public TokenPairResponse openSession(@Valid OpenDeviceSessionRequest request) {
        ClaimedDeviceChallenge challenge = authenticationAttemptService.claim(request);
        byte[] payload = DeviceSessionProof.payload(challenge.credentialId(),
                challenge.authSessionId(),
                challenge.challenge(),
                challenge.timestampEpochMillis());
        if (!signatureService.verify(challenge.publicKeyBase64(), payload, request.signatureBase64())) {
            throw new InvalidDeviceSignatureException();
        }
        return sessionIssuer.issue(challenge.credentialId());
    }

    /** Erneuert die Sitzung. */
    public TokenPairResponse refresh(@Valid RefreshDeviceSessionRequest request) {
        DeviceCredentialEntity credential = credentialRepository.findActiveCredential(request.deviceCredentialId())
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        Duration clockDifference = Duration.between(Instant.ofEpochMilli(request.timestampEpochMillis()), now).abs();
        if (clockDifference.compareTo(allowedClockSkew) > 0) {
            throw new InvalidRefreshTokenException();
        }
        byte[] tokenHash = sha256Hasher.hashAscii(request.refreshToken());
        byte[] proof =
                DeviceRefreshProof.payload(request.deviceCredentialId(), tokenHash, request.timestampEpochMillis());
        if (!signatureService.verify(credential.getPublicKey(), proof, request.signatureBase64())) {
            throw new InvalidRefreshTokenException();
        }
        RefreshRotationResult rotation =
                refreshTokenService.rotate(request.deviceCredentialId(), request.refreshToken());
        if (rotation.status() != RefreshRotationStatus.ROTATED) {
            throw new InvalidRefreshTokenException();
        }
        return new TokenPairResponse(rotation.accessToken().accessToken(),
                rotation.refreshToken().refreshToken(),
                rotation.accessToken().expiresAt(),
                rotation.refreshToken().expiresAt());
    }

    /** Beendet die Sitzung. */
    public void logout(UUID authenticationCredentialId, long authenticatedSessionVersion) {
        terminationService.logout(authenticationCredentialId, authenticatedSessionVersion);
    }

    /** Prüft den Aktivstatus. */
    private boolean isActive(DeviceCredentialEntity credential) {
        return credential.isActive()
                && credential.getDevice().getDeviceStatus() == DeviceStatus.ACTIVE
                && credential.getDevice().getUser().getStatus() == UserStatus.ACTIVE;
    }
}
