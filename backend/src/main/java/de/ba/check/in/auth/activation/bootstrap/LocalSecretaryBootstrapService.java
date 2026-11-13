package de.ba.check.in.auth.activation.bootstrap;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import de.ba.check.in.auth.activation.domain.ActivationMode;
import de.ba.check.in.auth.activation.repository.ActivationCodeRepository;
import de.ba.check.in.auth.activation.service.ActivationCodeHasher;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
@Profile("dev")
public class LocalSecretaryBootstrapService {
    private static final String CODE_PATTERN = "[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{16}";

    private final UserRepository userRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final ActivationCodeHasher activationCodeHasher;
    private final SecurityAuditService auditService;
    private final Clock clock;
    private final Duration activationCodeTtl;

    /** Erstellt den Dienst. */
    public LocalSecretaryBootstrapService(UserRepository userRepository,
            DeviceCredentialRepository credentialRepository,
            ActivationCodeRepository activationCodeRepository,
            ActivationCodeHasher activationCodeHasher,
            SecurityAuditService auditService,
            Clock clock,
            @Value("${app.security.activation-code.ttl:PT30M}") Duration activationCodeTtl) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.activationCodeHasher = activationCodeHasher;
        this.auditService = auditService;
        this.clock = clock;
        this.activationCodeTtl = requirePositive(activationCodeTtl, "app.security.activation-code.ttl");
    }

    /** Richtet den Zugang ein. */
    @Transactional
    public LocalBootstrapResult provision(UUID secretaryUserId, String rawActivationCode) {
        String normalizedCode = normalizeAndValidate(rawActivationCode);
        ensureBootstrapIsAvailable();
        Instant now = clock.instant();
        byte[] codeHash = activationCodeHasher.hash(normalizedCode);
        ActivationCodeEntity existing = activationCodeRepository.findByCodeHashForUpdate(codeHash).orElse(null);
        if (existing != null) {
            return reuseExistingCode(existing, secretaryUserId, now);
        }
        return createBootstrapCode(lockEligibleSecretary(secretaryUserId), secretaryUserId, codeHash, now);
    }

    /** Vereinheitlicht und prüft die Eingabe. */
    private String normalizeAndValidate(String rawActivationCode) {
        String normalizedCode = activationCodeHasher.normalize(rawActivationCode);
        if (!normalizedCode.matches(CODE_PATTERN)) {
            throw new IllegalStateException("BOOTSTRAP_ACTIVATION_CODE must contain exactly 16 valid code characters");
        }
        return normalizedCode;
    }

    /** Sperrt das Sekretariat für die Bearbeitung. */
    private UserEntity lockEligibleSecretary(UUID secretaryUserId) {
        UserEntity secretary = userRepository.findByIdForUpdate(secretaryUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "BOOTSTRAP_SECRETARY_USER_ID does not reference an existing user"));
        if (secretary.getRole() != UserRole.SECRETARY || secretary.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("The local bootstrap user must be an active Secretary");
        }
        return secretary;
    }

    /** Prüft, ob die Erstfreischaltung möglich ist. */
    private void ensureBootstrapIsAvailable() {
        if (credentialRepository.hasActiveCredentialForRole(UserRole.SECRETARY)) {
            throw new IllegalStateException("Local bootstrap is disabled after the first Secretary device is active");
        }
    }

    /** Verwendet einen bestehenden Code erneut. */
    private LocalBootstrapResult reuseExistingCode(ActivationCodeEntity existing, UUID secretaryUserId, Instant now) {
        if (existing.getUser().getId().equals(secretaryUserId)
                && existing.getMode() == ActivationMode.ADD_DEVICE
                && existing.isUsableAt(now)) {
            return new LocalBootstrapResult(existing.getId(), existing.getExpiresAt(), false);
        }
        throw new IllegalStateException("BOOTSTRAP_ACTIVATION_CODE was already used; configure a new code");
    }

    /** Erstellt einen Erstfreischaltcode. */
    private LocalBootstrapResult createBootstrapCode(UserEntity secretary,
            UUID secretaryUserId,
            byte[] codeHash,
            Instant now) {
        activationCodeRepository.revokeOpenCodes(secretaryUserId, now);
        ActivationCodeEntity activationCode = ActivationCodeEntity.create(secretary,
                secretary,
                codeHash,
                ActivationMode.ADD_DEVICE,
                now.plus(activationCodeTtl));
        activationCodeRepository.save(activationCode);
        recordCreationAudit(secretary, secretaryUserId, activationCode);
        return new LocalBootstrapResult(activationCode.getId(), activationCode.getExpiresAt(), true);
    }

    /** Protokolliert die Erstellung. */
    private void recordCreationAudit(UserEntity secretary, UUID secretaryUserId, ActivationCodeEntity activationCode) {
        auditService.record(AuditAction.ACTIVATION_CODE_CREATED,
                secretary,
                null,
                "USER",
                secretaryUserId,
                "mode=ADD_DEVICE;activationCodeId=%s;source=LOCAL_BOOTSTRAP".formatted(activationCode.getId()));
    }
}
