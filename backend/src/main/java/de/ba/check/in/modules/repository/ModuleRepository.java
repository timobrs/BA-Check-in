package de.ba.check.in.modules.repository;

import de.ba.check.in.modules.domain.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {

    /** Sucht passende Module. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select module from ModuleEntity module where module.id = :id")
    Optional<ModuleEntity> findByIdForUpdate(@Param("id") UUID id);

    /** Sucht passende Module. */
    Optional<ModuleEntity> findByCourseGroupIdAndNormalizedName(UUID courseGroupId, String normalizedName);

    /** Sucht passende Module. */
    List<ModuleEntity> findByCourseGroupId(UUID courseGroupId);

    /** Sucht passende Module. */
    List<ModuleEntity> findByActiveTrueOrderByCourseGroupNameAscNameAsc();
}
