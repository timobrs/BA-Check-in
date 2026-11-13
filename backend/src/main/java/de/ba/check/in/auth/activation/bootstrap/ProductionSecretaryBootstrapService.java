package de.ba.check.in.auth.activation.bootstrap;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import de.ba.check.in.auth.activation.domain.ActivationMode;
import de.ba.check.in.auth.activation.repository.ActivationCodeRepository;
import de.ba.check.in.auth.activation.service.ActivationCodeGenerator;
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

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
@Profile("production-bootstrap")
public class ProductionSecretaryBootstrapService {
    private final UserRepository userRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final ActivationCodeGenerator codeGenerator;
    private final ActivationCodeHasher codeHasher;
    private final SecurityAuditService auditService;
    private final Clock clock;
    private final Duration activationCodeTtl;

    /** Erstellt den Dienst. */
    public ProductionSecretaryBootstrapService(UserRepository userRepository,
            DeviceCredentialRepository credentialRepository,
            ActivationCodeRepository activationCodeRepository,
            ActivationCodeGenerator codeGenerator,
            ActivationCodeHasher codeHasher,
            SecurityAuditService auditService,
            Clock clock,
            @Value("${app.security.activation-code.ttl:PT30M}") Duration activationCodeTtl) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.auditService = auditService;
        this.clock = clock;
        this.activationCodeTtl = requirePositive(activationCodeTtl, "app.security.activation-code.ttl");
    }

    /** Richtet den Zugang ein. */
    @Transactional
    public ProductionBootstrapResult provision(String firstName, String lastName, String email) {
        Instant now = clock.instant();
        UserEntity secretary = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(UserEntity.create(firstName, lastName, email, UserRole.SECRETARY)));
        if (secretary.getRole() != UserRole.SECRETARY || secretary.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("The bootstrap email must identify an active Secretary");
        }
        if (credentialRepository.hasActiveCredentialForRole(UserRole.SECRETARY)) {
            throw new IllegalStateException("Production bootstrap is disabled after the first Secretary device is active");
        }
        activationCodeRepository.revokeOpenCodes(secretary.getId(), now);
        String rawCode = codeGenerator.generate();
        ActivationCodeEntity activationCode = ActivationCodeEntity.create(secretary, secretary,
                codeHasher.hash(rawCode), ActivationMode.ADD_DEVICE, now.plus(activationCodeTtl));
        activationCodeRepository.save(activationCode);
        auditService.record(AuditAction.ACTIVATION_CODE_CREATED, secretary, null, "USER", secretary.getId(),
                "mode=ADD_DEVICE;activationCodeId=%s;source=PRODUCTION_BOOTSTRAP".formatted(activationCode.getId()));
        return new ProductionBootstrapResult(secretary.getId(), rawCode, activationCode.getExpiresAt());
    }
}
