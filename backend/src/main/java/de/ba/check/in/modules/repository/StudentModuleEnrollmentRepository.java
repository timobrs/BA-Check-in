package de.ba.check.in.modules.repository;

import de.ba.check.in.modules.domain.StudentModuleEnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentModuleEnrollmentRepository extends JpaRepository<StudentModuleEnrollmentEntity, UUID> {

    /** Sucht passende Modulzuordnungen. */
    Optional<StudentModuleEnrollmentEntity> findByStudentIdAndModuleId(UUID studentId, UUID moduleId);

    /** Sucht passende Modulzuordnungen. */
    List<StudentModuleEnrollmentEntity> findByStudentId(UUID studentId);

    /** Sucht passende Modulzuordnungen. */
    List<StudentModuleEnrollmentEntity> findByModuleId(UUID moduleId);

    /** Sucht Studierende eines Moduls. */
    @Query("select enrollment.student.id from StudentModuleEnrollmentEntity enrollment where enrollment.module.id = :moduleId")
    List<UUID> findStudentIdsByModuleId(@Param("moduleId") UUID moduleId);
}
