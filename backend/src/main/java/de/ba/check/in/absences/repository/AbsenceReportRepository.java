package de.ba.check.in.absences.repository;

import de.ba.check.in.absences.domain.AbsenceReportEntity;
import de.ba.check.in.absences.domain.AbsenceReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AbsenceReportRepository extends JpaRepository<AbsenceReportEntity, UUID> {

    /** Sucht passende Abwesenheitsmeldungen. */
    List<AbsenceReportEntity> findByStudentIdOrderByStartsAtDesc(UUID studentId);

    /** Sucht passende Abwesenheitsmeldungen. */
    List<AbsenceReportEntity> findByStatusAndEndsAtAfter(AbsenceReportStatus status, Instant now);

    /** Sucht passende Abwesenheitsmeldungen. */
    List<AbsenceReportEntity> findByStudentIdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID studentId, Instant until, Instant from);
}
