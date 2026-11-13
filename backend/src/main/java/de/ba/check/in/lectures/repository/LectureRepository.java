package de.ba.check.in.lectures.repository;

import de.ba.check.in.lectures.domain.LectureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface LectureRepository extends JpaRepository<LectureEntity, UUID> {

    /** Sucht zukünftige Termine für die Moduländerung. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lecture from LectureEntity lecture
            where lecture.module.id = :moduleId and lecture.startsAt > :now
              and lecture.openedAt is null and lecture.attendanceFinalizedAt is null
              and lecture.lecturerOverride = false
            order by lecture.id
            """)
    List<LectureEntity> findFutureForModuleUpdate(@Param("moduleId") UUID moduleId, @Param("now") Instant now);

    /** Sucht passende Termine. */
    List<LectureEntity> findByCourseGroupIdOrderByStartsAt(UUID courseGroupId);

    /** Prüft, ob eine Anwesenheit vorliegt. */
    @Query("""
            select count(lecture) > 0 from LectureEntity lecture
            where lecture.id = :id and (
                exists (select record.id from AttendanceRecordEntity record where record.lecture = lecture)
                or exists (select event.id from AttendanceEventEntity event where event.lecture = lecture)
            )
            """)
    boolean hasAttendance(@Param("id") UUID id);

    /** Sucht passende Termine. */
    List<LectureEntity> findByCourseGroupIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(UUID courseGroupId, Instant now);

    /** Sucht passende Termine. */
    List<LectureEntity> findByLecturerUserIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(UUID lecturerId, Instant now);

    /** Sucht passende Termine. */
    List<LectureEntity> findByActiveTrueOrderByStartsAt();

    /** Sucht einen importierten Kalendereintrag. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lecture from LectureEntity lecture
            where lecture.trainexSourceKey = :source
              and lecture.externalReference = :eventId
            """)
    Optional<LectureEntity> findTrainexEvent(
            @Param("source") String calendarKey,
            @Param("eventId") String eventId);

    /** Sucht aktive importierte Termine. */
    @Query("""
            select lecture from LectureEntity lecture
            where lecture.trainexSourceKey = :source
              and lecture.source = de.ba.check.in.lectures.domain.LectureSource.TRAINEX
              and lecture.active = true
              and lecture.endsAt > :now
            """)
    List<LectureEntity> findActiveTrainex(
            @Param("source") String calendarKey,
            @Param("now") Instant now);

    /** Sucht passende Termine. */
    Optional<LectureEntity> findFirstByCourseGroupIdAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByStartsAtAsc(
            UUID courseGroupId, Instant startsAt, Instant endsAt);

    /** Sucht passende Termine. */
    List<LectureEntity> findByModuleIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(UUID moduleId, Instant now);

    /** Sucht passende Termine. */
    List<LectureEntity> findByAttendanceFinalizedAtIsNullAndEndsAtLessThanEqualOrderByEndsAt(Instant now);

    /** Sucht passende Termine. */
    List<LectureEntity> findByActiveTrueAndStartsAtLessThanAndEndsAtGreaterThan(Instant until, Instant from);

    /** Sucht passende Termine. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lecture from LectureEntity lecture where lecture.id = :id")
    Optional<LectureEntity> findByIdForUpdate(@Param("id") UUID id);

    /** Sucht passende Termine im Check-in-Zeitraum. */
    @Query("""
            select distinct lecture from LectureEntity lecture
            left join StudentModuleEnrollmentEntity enrollment
              on enrollment.module = lecture.module and enrollment.student.id = :studentId
            where lecture.active = true
              and (lecture.courseGroup.id = :courseGroupId or enrollment.id is not null)
              and lecture.startsAt <= :latestStart
              and lecture.endsAt > :now
            order by lecture.startsAt
            """)
    List<LectureEntity> findEligibleInCheckInWindow(
            @Param("studentId") UUID studentId,
            @Param("courseGroupId") UUID courseGroupId,
            @Param("now") Instant now,
            @Param("latestStart") Instant latestStart);

    /** Sucht bevorstehende passende Termine. */
    @Query("""
            select distinct lecture from LectureEntity lecture
            left join StudentModuleEnrollmentEntity enrollment
              on enrollment.module = lecture.module and enrollment.student.id = :studentId
            where lecture.active = true
              and (lecture.courseGroup.id = :courseGroupId or enrollment.id is not null)
              and lecture.endsAt > :now
            order by lecture.startsAt
            """)
    List<LectureEntity> findEligibleUpcoming(
            @Param("studentId") UUID studentId,
            @Param("courseGroupId") UUID courseGroupId,
            @Param("now") Instant now);
}
