package de.ba.check.in.users.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends TimestampedEntity {

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    /** Erstellt einen Nutzer. */
    public static UserEntity create(
            String firstName,
            String lastName,
            String email,
            UserRole role) {
        UserEntity entity = new UserEntity();
        entity.firstName = firstName;
        entity.lastName = lastName;
        entity.email = email;
        entity.role = role;
        entity.status = UserStatus.ACTIVE;
        return entity;
    }
}
