package de.ba.check.in.auth.activation.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "activation_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivationCodeEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "code_hash", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 50)
    private ActivationMode mode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Erstellt einen Aktivierungscode. */
    public static ActivationCodeEntity create(
            UserEntity user,
            UserEntity createdBy,
            byte[] codeHash,
            ActivationMode mode,
            Instant expiresAt) {
        ActivationCodeEntity entity = new ActivationCodeEntity();
        entity.user = user;
        entity.createdBy = createdBy;
        entity.codeHash = codeHash.clone();
        entity.mode = mode;
        entity.expiresAt = expiresAt;
        return entity;
    }

    /** Prüft die Nutzbarkeit zum angegebenen Zeitpunkt. */
    public boolean isUsableAt(Instant now) {
        return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    /** Markiert den Wert als verwendet. */
    public void consume(Instant now) {
        consumedAt = now;
    }

}
