package de.ba.check.in.auth.activation.service;

import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import de.ba.check.in.auth.activation.domain.ActivationSessionEntity;
import de.ba.check.in.auth.activation.dto.ActivationChallengeResponse;
import de.ba.check.in.auth.activation.dto.ActivationResponse;
import de.ba.check.in.auth.activation.dto.CompleteActivationRequest;
import de.ba.check.in.auth.activation.exception.ActivationCodeNotUsableException;
import de.ba.check.in.auth.activation.repository.ActivationCodeRepository;
import de.ba.check.in.auth.activation.repository.ActivationSessionRepository;
import de.ba.check.in.auth.devicekey.exception.DeviceKeyRejectedException;
import de.ba.check.in.auth.devicekey.model.VerifiedDeviceKey;
import de.ba.check.in.auth.devicekey.service.DeviceKeyProofVerifier;
import de.ba.check.in.users.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
@Validated
public class ActivationService {
    private static final int CHALLENGE_LENGTH_BYTES = 32;
    private static final String UNIQUE_DEVICE_KEY_CONSTRAINT = "uq_device_credentials_public_key_fingerprint";

    private final ActivationCodeRepository activationCodeRepository;
    private final ActivationSessionRepository activationSessionRepository;
    private final ActivationCodeHasher activationCodeHasher;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration challengeTtl;
    private final DeviceKeyProofVerifier deviceKeyProofVerifier;
    private final ActivationCompletionService completionService;

    /** Erstellt den Dienst. */
    public ActivationService(ActivationCodeRepository activationCodeRepository,
            ActivationSessionRepository activationSessionRepository,
            ActivationCodeHasher activationCodeHasher,
            SecureRandom secureRandom,
            Clock clock,
            DeviceKeyProofVerifier deviceKeyProofVerifier,
            ActivationCompletionService completionService,
            @Value("${app.security.activation-code.challenge-ttl:PT2M}") Duration challengeTtl) {
        this.activationCodeRepository = activationCodeRepository;
        this.activationSessionRepository = activationSessionRepository;
        this.activationCodeHasher = activationCodeHasher;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.deviceKeyProofVerifier = deviceKeyProofVerifier;
        this.completionService = completionService;
        this.challengeTtl = requirePositive(challengeTtl, "app.security.activation-code.challenge-ttl");
    }

    /** Erstellt eine Challenge. */
    @Transactional
    public ActivationChallengeResponse createChallenge(@NotBlank String rawActivationCode) {
        Instant now = clock.instant();
        String normalizedCode = activationCodeHasher.normalize(rawActivationCode);
        if (!normalizedCode.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{16}")) {
            throw new ActivationCodeNotUsableException();
        }
        ActivationCodeEntity activationCode =
                activationCodeRepository.findByCodeHashForUpdate(activationCodeHasher.hash(normalizedCode))
                        .orElseThrow(ActivationCodeNotUsableException::new);
        if (!activationCode.isUsableAt(now) || activationCode.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ActivationCodeNotUsableException();
        }
        activationSessionRepository.invalidateOpenSessions(activationCode.getId(), now);
        byte[] challenge = new byte[CHALLENGE_LENGTH_BYTES];
        secureRandom.nextBytes(challenge);
        Instant expiresAt = now.plus(challengeTtl);
        ActivationSessionEntity session = ActivationSessionEntity.create(activationCode, challenge, expiresAt);
        activationSessionRepository.save(session);
        return new ActivationChallengeResponse(session.getId(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(challenge),
                expiresAt);
    }

    /** Schließt die Geräteaktivierung ab. */
    public ActivationResponse complete(@Valid CompleteActivationRequest request) {
        ActivationPreparation preparation = completionService.prepare(request.activationSessionId());
        VerifiedDeviceKey deviceKey = deviceKeyProofVerifier.verify(request, preparation.challenge());
        try {
            return completionService.complete(request, preparation, deviceKey);
        } catch (DataIntegrityViolationException exception) {
            if (!isConstraintViolation(exception)) {
                throw exception;
            }
            throw new DeviceKeyRejectedException("Device key is already registered");
        }
    }

    /** Prüft auf einen Datenbankkonflikt. */
    private boolean isConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && ActivationService.UNIQUE_DEVICE_KEY_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
