package de.ba.check.in.students.repository;

import de.ba.check.in.students.domain.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface StudentRepository extends JpaRepository<StudentEntity, UUID> {

    /** Sucht passende Studierende. */
    Optional<StudentEntity> findByUserId(UUID userId);

    /** Sucht passende Studierende. */
    Optional<StudentEntity> findByUserEmailIgnoreCase(String email);

    /** Sucht passende Studierende. */
    Optional<StudentEntity> findByMatriculationNumberIgnoreCase(String matriculationNumber);

    /** Sucht passende Studierende. */
    List<StudentEntity> findByCourseGroupIdOrderByUserLastNameAsc(UUID courseGroupId);

    /** Sucht passende Studierende. */
    List<StudentEntity> findByIdIn(Collection<UUID> ids);
}
