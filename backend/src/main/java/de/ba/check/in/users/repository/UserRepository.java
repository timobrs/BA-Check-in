package de.ba.check.in.users.repository;

import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import de.ba.check.in.users.domain.UserRole;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /** Sucht passende Nutzer. */
    Optional<UserEntity> findByEmail(String email);

    /** Sucht passende Nutzer. */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /** Sucht passende Nutzer. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") UUID userId);

    /** Sucht passende Nutzer. */
    List<UserEntity> findByRole(UserRole role);
}
