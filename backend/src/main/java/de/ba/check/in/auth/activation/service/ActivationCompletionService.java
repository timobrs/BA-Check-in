package de.ba.check.in.auth.activation.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.activation.domain.ActivationCodeEntity;
import de.ba.check.in.auth.activation.domain.ActivationMode;
import de.ba.check.in.auth.activation.domain.ActivationSessionEntity;
import de.ba.check.in.auth.activation.dto.ActivationResponse;
import de.ba.check.in.auth.activation.dto.CompleteActivationRequest;
import de.ba.check.in.auth.activation.exception.ActivationModeNotAllowedException;
import de.ba.check.in.auth.activation.exception.ActivationSessionNotUsableException;
import de.ba.check.in.auth.activation.exception.ActiveDeviceExistsException;
import de.ba.check.in.auth.activation.repository.ActivationCodeRepository;
import de.ba.check.in.auth.activation.repository.ActivationSessionRepository;
import de.ba.check.in.auth.devicekey.exception.DeviceKeyRejectedException;
import de.ba.check.in.auth.devicekey.model.VerifiedDeviceKey;
import de.ba.check.in.auth.token.domain.RefreshTokenRevocationReason;
import de.ba.check.in.auth.token.repository.RefreshTokenRepository;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.devices.repository.DeviceRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivationCompletionService {
    private final ActivationSessionRepository activationSessionRepository;
    private final ActivationCodeRepository activationCodeRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditService auditService;
    private final Clock clock;

    /** Bereitet die Geräteaktivierung vor. */
    @Transactional(readOnly = true)
    public ActivationPreparation prepare(UUID activationSessionId) {
        ActivationSessionEntity session = activationSessionRepository.findForVerification(activationSessionId)
                .orElseThrow(ActivationSessionNotUsableException::new);
        ActivationCodeEntity code = session.getActivationCode();
        return new ActivationPreparation(code.getUser().getId(), code.getId(), session.getChallenge());
    }

    /** Schließt die Geräteaktivierung ab. */
    @Transactional
    public ActivationResponse complete(CompleteActivationRequest request,
            ActivationPreparation preparation,
            VerifiedDeviceKey deviceKey) {
        Instant now = clock.instant();
        LockedActivation activation = lockActivation(request, preparation);
        verifyBinding(activation, preparation);
        Optional<DeviceCredentialEntity> existingCredential = completedCredential(activation.session(), deviceKey);
        if (existingCredential.isPresent()) {
            return response(activation.user(), existingCredential.get());
        }
        validateUsable(activation, now);
        ensurePublicKeyIsAvailable(deviceKey);
        ReplacementPlan replacement = planReplacement(activation.user(), activation.code().getMode());
        applyReplacement(replacement, activation.code().getCreatedBy(), now);
        ActivatedDevice activated = registerDevice(activation, request, deviceKey, now);
        finalizeActivation(activation, activated.credential(), now);
        recordAudit(activation, replacement, activated);
        return response(activation.user(), activated.credential());
    }

    /** Sperrt die Aktivierung für die Bearbeitung. */
    private LockedActivation lockActivation(CompleteActivationRequest request, ActivationPreparation preparation) {
        UserEntity user = userRepository.findByIdForUpdate(preparation.userId())
                .orElseThrow(ActivationSessionNotUsableException::new);
        ActivationCodeEntity code = activationCodeRepository.findByIdForUpdate(preparation.activationCodeId())
                .orElseThrow(ActivationSessionNotUsableException::new);
        ActivationSessionEntity session = activationSessionRepository.findByIdForUpdate(request.activationSessionId())
                .orElseThrow(ActivationSessionNotUsableException::new);
        return new LockedActivation(user, code, session);
    }

    /** Lädt den abgeschlossenen Gerätezugang. */
    private Optional<DeviceCredentialEntity> completedCredential(ActivationSessionEntity session,
            VerifiedDeviceKey deviceKey) {
        DeviceCredentialEntity result = session.getResultDeviceCredential();
        if (result == null) {
            return Optional.empty();
        }
        if (!MessageDigest.isEqual(result.getPublicKeyFingerprint(), deviceKey.publicKeyFingerprint())) {
            throw new ActivationSessionNotUsableException();
        }
        return Optional.of(result);
    }

    /** Prüft, ob der Eintrag nutzbar ist. */
    private void validateUsable(LockedActivation activation, Instant now) {
        if (!activation.session().isUsableAt(now)
                || !activation.code().isUsableAt(now)
                || activation.user().getStatus() != UserStatus.ACTIVE) {
            throw new ActivationSessionNotUsableException();
        }
    }

    /** Prüft, ob der öffentliche Schlüssel frei ist. */
    private void ensurePublicKeyIsAvailable(VerifiedDeviceKey deviceKey) {
        if (credentialRepository.findByPublicKeyFingerprint(deviceKey.publicKeyFingerprint()).isPresent()) {
            throw new DeviceKeyRejectedException("Device key is already registered");
        }
    }

    /** Plant den Gerätewechsel. */
    private ReplacementPlan planReplacement(UserEntity user, ActivationMode mode) {
        List<DeviceEntity> activeDevices = deviceRepository.findActiveByUserIdForUpdate(user.getId());
        if (mode == ActivationMode.ADD_DEVICE && !activeDevices.isEmpty()) {
            throw new ActiveDeviceExistsException();
        }
        if (mode == ActivationMode.REPLACE_DEVICE && activeDevices.isEmpty()) {
            throw new ActivationModeNotAllowedException("A replacement activation requires an active device");
        }
        boolean replacesExistingDevices = mode == ActivationMode.REPLACE_DEVICE;
        List<DeviceCredentialEntity> credentialsToReplace =
                replacesExistingDevices ? credentialRepository.findActiveByUserIdForUpdate(user.getId()) : List.of();
        return new ReplacementPlan(replacesExistingDevices, activeDevices, credentialsToReplace);
    }

    /** Führt den Gerätewechsel aus. */
    private void applyReplacement(ReplacementPlan replacement, UserEntity actor, Instant now) {
        if (!replacement.replacesExistingDevices()) {
            return;
        }
        replaceActiveDevices(replacement.devices(), replacement.credentials(), actor, now);
        deviceRepository.flush();
        credentialRepository.flush();
    }

    /** Registriert ein Gerät. */
    private ActivatedDevice registerDevice(LockedActivation activation,
            CompleteActivationRequest request,
            VerifiedDeviceKey deviceKey,
            Instant now) {
        DeviceEntity newDevice = DeviceEntity.activate(activation.user(),
                request.deviceName().trim(),
                request.platform(),
                trimToNull(request.manufacturer()),
                trimToNull(request.model()),
                trimToNull(request.osVersion()),
                trimToNull(request.appVersion()),
                activation.code().getCreatedBy(),
                now);
        deviceRepository.save(newDevice);
        DeviceCredentialEntity newCredential = DeviceCredentialEntity.create(newDevice,
                deviceKey.publicKeyBase64(),
                deviceKey.publicKeyFingerprint(),
                deviceKey.algorithm());
        credentialRepository.save(newCredential);
        return new ActivatedDevice(newDevice, newCredential);
    }

    /** Schließt die Geräteaktivierung ab. */
    private void finalizeActivation(LockedActivation activation, DeviceCredentialEntity credential, Instant now) {
        activation.code().consume(now);
        activation.session().complete(credential, now);
    }

    /** Schreibt einen Audit-Eintrag. */
    private void recordAudit(LockedActivation activation, ReplacementPlan replacement, ActivatedDevice activated) {
        auditService.record(AuditAction.DEVICE_ACTIVATED,
                activation.user(),
                activated.device(),
                "DEVICE",
                activated.device().getId(),
                "mode=%s;credentialId=%s".formatted(activation.code().getMode(), activated.credential().getId()));
        if (replacement.replacesExistingDevices()) {
            for (DeviceEntity replacedDevice : replacement.devices()) {
                auditService.record(AuditAction.DEVICE_REPLACED,
                        activation.user(),
                        activated.device(),
                        "DEVICE",
                        replacedDevice.getId(),
                        "replacementDeviceId=%s".formatted(activated.device().getId()));
            }
        }
    }

    /** Prüft die Gerätebindung. */
    private void verifyBinding(LockedActivation activation, ActivationPreparation preparation) {
        if (!activation.session().getActivationCode().getId().equals(activation.code().getId()) || !activation.code()
                .getUser().getId().equals(activation.user().getId()) || !MessageDigest.isEqual(activation.session()
                .getChallenge(), preparation.challenge())) {
            throw new ActivationSessionNotUsableException();
        }
    }

    /** Ersetzt aktive Gerätebindungen. */
    private void replaceActiveDevices(List<DeviceEntity> activeDevices,
            List<DeviceCredentialEntity> credentials,
            UserEntity actor,
            Instant now) {
        for (DeviceEntity device : activeDevices) {
            device.replace(actor, now);
        }
        for (DeviceCredentialEntity credential : credentials) {
            credential.deactivate();
            refreshTokenRepository.revokeForCredential(credential.getId(),
                    now,
                    RefreshTokenRevocationReason.DEVICE_REPLACED);
        }
    }

    /** Erstellt die Antwort. */
    private ActivationResponse response(UserEntity user, DeviceCredentialEntity credential) {
        String displayName =
                ("%s %s".formatted(user.getFirstName() == null ? "" : user.getFirstName(), user.getLastName())).trim();
        return new ActivationResponse(user.getId(),
                user.getRole(),
                displayName,
                credential.getDevice().getId(),
                credential.getId());
    }

    /** Entfernt Leerzeichen und leere Werte. */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record LockedActivation(
            UserEntity user,
            ActivationCodeEntity code,
            ActivationSessionEntity session) {
    }

    private record ReplacementPlan(
            boolean replacesExistingDevices,
            List<DeviceEntity> devices,
            List<DeviceCredentialEntity> credentials) {
    }

    private record ActivatedDevice(
            DeviceEntity device,
            DeviceCredentialEntity credential) {
    }
}
