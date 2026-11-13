package de.ba.check.in.auth.token.service;

import de.ba.check.in.audit.domain.AuditAction;
import de.ba.check.in.audit.service.SecurityAuditService;
import de.ba.check.in.auth.crypto.Sha256Hasher;
import de.ba.check.in.auth.token.domain.RefreshTokenEntity;
import de.ba.check.in.auth.token.model.IssuedAccessToken;
import de.ba.check.in.auth.token.model.IssuedRefreshToken;
import de.ba.check.in.auth.token.model.RefreshRotationResult;
import de.ba.check.in.auth.token.repository.RefreshTokenRepository;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceStatus;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.users.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
public class RefreshTokenService {
    private static final int TOKEN_LENGTH_BYTES = 32;

    private final DeviceCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Sha256Hasher sha256Hasher;
    private final JwtService jwtService;
    private final SecurityAuditService auditService;
    private final Duration idleTtl;
    private final Duration familyTtl;

    /** Erstellt den Dienst. */
    public RefreshTokenService(DeviceCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            SecureRandom secureRandom,
            Clock clock,
            Sha256Hasher sha256Hasher,
            JwtService jwtService,
            SecurityAuditService auditService,
            @Value("${app.security.refresh-token.idle-ttl:P7D}") Duration idleTtl,
            @Value("${app.security.refresh-token.family-ttl:P30D}") Duration familyTtl) {
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.sha256Hasher = sha256Hasher;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.idleTtl = requirePositive(idleTtl, "app.security.refresh-token.idle-ttl");
        this.familyTtl = requirePositive(familyTtl, "app.security.refresh-token.family-ttl");
    }

    /** Erstellt ein erstes Token. */
    @Transactional
    public IssuedRefreshToken createInitialToken(DeviceCredentialEntity credential) {
        Instant now = clock.instant();
        Instant familyExpiresAt = now.plus(familyTtl);
        Instant expiresAt = earlier(now.plus(idleTtl), familyExpiresAt);
        String rawToken = generateRawToken();
        byte[] tokenHash = hashToken(rawToken);
        RefreshTokenEntity refreshToken =
                RefreshTokenEntity.initial(UUID.randomUUID(), credential, tokenHash, expiresAt, familyExpiresAt);
        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    /** Erneuert das Token. */
    @Transactional
    public RefreshRotationResult rotate(UUID credentialId, String rawToken) {
        Instant now = clock.instant();
        Optional<RotationContext> lockedContext = lockRotationContext(credentialId, rawToken);
        if (lockedContext.isEmpty()) {
            return RefreshRotationResult.invalid();
        }
        RotationContext context = lockedContext.get();
        if (context.token().isRotated()) {
            return handleReplay(context, now);
        }
        if (!isUsable(context, now)) {
            return RefreshRotationResult.invalid();
        }
        return rotateUsableToken(context, now);
    }

    /** Sperrt den Kontext für die Token-Erneuerung. */
    private Optional<RotationContext> lockRotationContext(UUID credentialId, String rawToken) {
        DeviceCredentialEntity credential = credentialRepository.findByIdForUpdate(credentialId).orElse(null);
        if (credential == null) {
            return Optional.empty();
        }
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawToken)).orElse(null);
        if (current == null || !current.getDeviceCredential().getId().equals(credentialId)) {
            return Optional.empty();
        }
        return Optional.of(new RotationContext(credential, current));
    }

    /** Behandelt eine Token-Wiederverwendung. */
    private RefreshRotationResult handleReplay(RotationContext context, Instant now) {
        refreshTokenRepository.markFamilyAsReplayed(context.token().getFamilyId(), now);
        recordAudit(AuditAction.REFRESH_TOKEN_REPLAY_DETECTED, context);
        return RefreshRotationResult.replayDetected();
    }

    /** Prüft die Nutzbarkeit. */
    private boolean isUsable(RotationContext context, Instant now) {
        RefreshTokenEntity current = context.token();
        DeviceCredentialEntity credential = context.credential();
        return !current.isRevoked()
                && !current.isExpiredAt(now)
                && current.getSessionVersion() == credential.getSessionVersion()
                && isActive(credential);
    }

    /** Erneuert ein gültiges Token. */
    private RefreshRotationResult rotateUsableToken(RotationContext context, Instant now) {
        RefreshTokenEntity current = context.token();
        DeviceCredentialEntity credential = context.credential();
        Instant expiresAt = earlier(now.plus(idleTtl), current.getFamilyExpiresAt());
        if (!expiresAt.isAfter(now)) {
            return RefreshRotationResult.invalid();
        }
        String replacementRawToken = generateRawToken();
        current.consumeForRotation(now);
        refreshTokenRepository.flush();
        RefreshTokenEntity replacement =
                RefreshTokenEntity.replacement(current, hashToken(replacementRawToken), expiresAt);
        refreshTokenRepository.save(replacement);
        current.linkReplacement(replacement);
        credential.recordUse(now);
        IssuedAccessToken accessToken = jwtService.createAccessToken(credential);
        IssuedRefreshToken refreshToken = new IssuedRefreshToken(replacementRawToken, expiresAt);
        recordAudit(AuditAction.REFRESH_TOKEN_ROTATED, context);
        return RefreshRotationResult.rotated(accessToken, refreshToken);
    }

    /** Schreibt einen Audit-Eintrag. */
    private void recordAudit(AuditAction action, RotationContext context) {
        DeviceCredentialEntity credential = context.credential();
        auditService.record(action,
                credential.getDevice().getUser(),
                credential.getDevice(),
                "REFRESH_TOKEN_FAMILY",
                context.token().getFamilyId(),
                "credentialId=%s".formatted(credential.getId()));
    }

    /** Berechnet den Hash des Tokens. */
    public byte[] hashToken(String rawToken) {
        return sha256Hasher.hashAscii(rawToken);
    }

    /** Erzeugt einen Token-Wert. */
    String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /** Prüft den Aktivstatus. */
    private boolean isActive(DeviceCredentialEntity credential) {
        return credential.isActive()
                && credential.getDevice().getDeviceStatus() == DeviceStatus.ACTIVE
                && credential.getDevice().getUser().getStatus() == UserStatus.ACTIVE;
    }

    /** Gibt den früheren Zeitpunkt zurück. */
    private Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private record RotationContext(
            DeviceCredentialEntity credential,
            RefreshTokenEntity token) {
    }
}
