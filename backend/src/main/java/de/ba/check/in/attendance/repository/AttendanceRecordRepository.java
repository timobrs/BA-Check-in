package de.ba.check.in.attendance.repository;

import de.ba.check.in.attendance.domain.AttendanceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, UUID> {

    /** Sucht passende Anwesenheitseinträge. */
    Optional<AttendanceRecordEntity> findByStudentIdAndLectureId(UUID studentId, UUID lectureId);

    /** Sucht passende Anwesenheitseinträge. */
    List<AttendanceRecordEntity> findByLectureId(UUID lectureId);

    /** Sucht passende Anwesenheitseinträge. */
    Optional<AttendanceRecordEntity> findFirstByStudentIdAndCheckInTimeIsNotNullAndLectureEndsAtAfterOrderByCheckInTimeDesc(
            UUID studentId, Instant now);
}
