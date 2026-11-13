package de.ba.check.in.auth.activation.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import de.ba.check.in.auth.activation.domain.ActivationMode;
import de.ba.check.in.auth.activation.dto.CreatedActivationCodeResponse;
import de.ba.check.in.auth.activation.exception.ActivationModeNotAllowedException;
import de.ba.check.in.auth.activation.exception.ActiveDeviceExistsException;
import de.ba.check.in.auth.activation.repository.ActivationCodeRepository;
import de.ba.check.in.auth.exception.AccessDeniedApiException;
import de.ba.check.in.devices.repository.DeviceRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.exception.UserNotFoundException;
import de.ba.check.in.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
public class ActivationCodeService {
    private final ActivationCodeRepository activationCodeRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ActivationCodeGenerator activationCodeGenerator;
    private final ActivationCodeHasher activationCodeHasher;
    private final SecurityAuditService auditService;
    private final Clock clock;
    private final Duration activationCodeTtl;

    /** Erstellt den Dienst. */
    public ActivationCodeService(ActivationCodeRepository activationCodeRepository,
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            ActivationCodeGenerator activationCodeGenerator,
            ActivationCodeHasher activationCodeHasher,
            SecurityAuditService auditService,
            Clock clock,
            @Value("${app.security.activation-code.ttl:PT30M}") Duration activationCodeTtl) {
        this.activationCodeRepository = activationCodeRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.activationCodeGenerator = activationCodeGenerator;
        this.activationCodeHasher = activationCodeHasher;
        this.auditService = auditService;
        this.clock = clock;
        this.activationCodeTtl = requirePositive(activationCodeTtl, "app.security.activation-code.ttl");
    }

    /** Erstellt einen Aktivierungscode. */
    @Transactional
    public CreatedActivationCodeResponse createActivationCode(UUID userId, UUID secretaryId, ActivationMode mode) {
        Instant now = clock.instant();
        UserEntity targetUser =
                userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        UserEntity secretary = userRepository.findById(secretaryId)
                .orElseThrow(() -> new AccessDeniedApiException("Secretary user does not exist"));
        validateSecretary(secretary);
        validateTarget(targetUser, mode);
        activationCodeRepository.revokeOpenCodes(userId, now);
        String rawCode = activationCodeGenerator.generate();
        Instant expiresAt = now.plus(activationCodeTtl);
        ActivationCodeEntity activationCode =
                ActivationCodeEntity.create(targetUser, secretary, activationCodeHasher.hash(rawCode), mode, expiresAt);
        activationCodeRepository.save(activationCode);
        auditService.record(AuditAction.ACTIVATION_CODE_CREATED,
                secretary,
                null,
                "USER",
                userId,
                "mode=%s;activationCodeId=%s".formatted(mode, activationCode.getId()));
        return new CreatedActivationCodeResponse(activationCode.getId(), userId, rawCode, expiresAt, mode);
    }

    /** Erstellt einen automatischen Aktivierungscode. */
    @Transactional
    public CreatedActivationCodeResponse createAutomaticActivationCode(UUID userId, UUID secretaryId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        boolean hasActiveDevice = !deviceRepository.findActiveByUserIdForUpdate(userId).isEmpty();
        ActivationMode mode = hasActiveDevice
                ? ActivationMode.REPLACE_DEVICE
                : ActivationMode.ADD_DEVICE;
        return createActivationCode(userId, secretaryId, mode);
    }

    /** Sperrt offene Aktivierungscodes. */
    @Transactional
    public void revokeOpenCodes(UUID userId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        activationCodeRepository.revokeOpenCodes(userId, clock.instant());
    }

    /** Prüft den Sekretariatszugang. */
    private void validateSecretary(UserEntity secretary) {
        if (secretary.getRole() != UserRole.SECRETARY || secretary.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedApiException("Activation codes may only be created by an active secretary");
        }
    }

    /** Prüft das Ziel. */
    private void validateTarget(UserEntity targetUser, ActivationMode mode) {
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new ActivationModeNotAllowedException("Inactive users cannot activate devices");
        }

        boolean hasActiveDevice = !deviceRepository.findActiveByUserIdForUpdate(targetUser.getId()).isEmpty();

        if (mode == ActivationMode.ADD_DEVICE && hasActiveDevice) {
            throw new ActiveDeviceExistsException();
        }

        if (mode == ActivationMode.REPLACE_DEVICE && !hasActiveDevice) {
            throw new ActivationModeNotAllowedException("A user without an active device must use ADD_DEVICE");
        }
    }
}
