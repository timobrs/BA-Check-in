package de.ba.check.in.students.repository;

import de.ba.check.in.students.domain.CourseGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseGroupRepository extends JpaRepository<CourseGroupEntity, UUID> {

    /** Sucht passende Kurse. */
    Optional<CourseGroupEntity> findByNameIgnoreCase(String name);

    /** Sucht passende Kurse. */
    List<CourseGroupEntity> findByActiveTrueOrderByNameAsc();
}
