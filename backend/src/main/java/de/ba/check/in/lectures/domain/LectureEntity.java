package de.ba.check.in.lectures.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.students.domain.CourseGroupEntity;
import de.ba.check.in.modules.domain.ModuleEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import de.ba.check.in.students.domain.StudentEntity;

@Entity
@Table(name = "lectures")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureEntity extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_group_id", nullable = false)
    private CourseGroupEntity courseGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_user_id")
    private UserEntity lecturerUser;

    @Column(name = "lecturer_override", nullable = false)
    private boolean lecturerOverride = false;

    /** Übernimmt den Dozenten des Moduls. */
    public void useModuleLecturer() {
        if (!lecturerOverride) {
            lecturerUser = module.getLecturerUser();
        }
    }

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "room", length = 100)
    private String room;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private LectureSource source = LectureSource.MANUAL;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "ignored", nullable = false)
    private boolean ignored = false;

    @Column(name = "trainex_source_key", length = 64)
    private String trainexSourceKey;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "last_punctual_check_in_at")
    private Instant lastPunctualCheckInAt;

    @Column(name = "late_phase_started_at")
    private Instant latePhaseStartedAt;

    @Column(name = "attendance_finalized_at")
    private Instant attendanceFinalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protokollant_student_id")
    private StudentEntity protokollant;

    @Column(name = "attendance_validated_at")
    private Instant attendanceValidatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_validated_by")
    private UserEntity attendanceValidatedBy;

    /** Erstellt einen manuellen Termin. */
    public static LectureEntity createManual(CourseGroupEntity course) {
        LectureEntity entity = new LectureEntity();
        entity.courseGroup = course;
        return entity;
    }

    /** Erstellt einen importierten Termin. */
    public static LectureEntity createTrainex(CourseGroupEntity course, String sourceKey, String uid) {
        LectureEntity entity = new LectureEntity();
        entity.courseGroup = course;
        entity.source = LectureSource.TRAINEX;
        entity.trainexSourceKey = sourceKey;
        entity.externalReference = uid;
        entity.active = true;
        return entity;
    }

    /** Öffnet den Termin. */
    public void open(Instant now) { openedAt = now; }

    /** Bestätigt die Anwesenheiten. */
    public void validateAttendances(UserEntity user, Instant now) { attendanceValidatedBy = user; attendanceValidatedAt = now; }

    /** Setzt die Bestätigung der Anwesenheiten zurück. */
    public void invalidateAttendances() { attendanceValidatedBy = null; attendanceValidatedAt = null; }

    /** Erfasst den pünktlichen Check-in. */
    public void recordPunctualCheckIn(Instant at) { lastPunctualCheckInAt = at; }

    /** Startet die Phase für verspäteten Check-in. */
    public void startLatePhase(Instant at) { if (latePhaseStartedAt == null) latePhaseStartedAt = at; }

    /** Markiert die Anwesenheiten als abgeschlossen. */
    public void markAttendanceFinalized(Instant at) { attendanceFinalizedAt = at; }

    /** Archiviert den Termin. */
    public void archive() { active = false; }

    /** Prüft die Relevanz. */
    public boolean isRelevant() {
        return !ignored && module != null && !module.isIgnored();
    }
}
