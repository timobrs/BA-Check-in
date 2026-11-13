package de.ba.check.in.absences.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "absence_reports")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbsenceReportEntity extends TimestampedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AbsenceReportStatus status = AbsenceReportStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserEntity reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Erstellt eine Abwesenheitsmeldung. */
    public static AbsenceReportEntity create(StudentEntity student, Instant startsAt, Instant endsAt) {
        AbsenceReportEntity entity = new AbsenceReportEntity();
        entity.student = student;
        entity.startsAt = startsAt;
        entity.endsAt = endsAt;
        return entity;
    }

    /** Prüft die Abwesenheitsmeldung. */
    public void review(AbsenceReportStatus decision, UserEntity actor, Instant now) {
        if (decision != AbsenceReportStatus.APPROVED && decision != AbsenceReportStatus.REJECTED) {
            throw new IllegalArgumentException("Only APPROVED or REJECTED are review decisions");
        }
        status = decision;
        reviewedBy = actor;
        reviewedAt = now;
    }

    /** Prüft, ob sich die Zeiträume überschneiden. */
    public boolean overlaps(Instant from, Instant until) {
        return startsAt.isBefore(until) && endsAt.isAfter(from);
    }

    /** Prüft, ob der Zeitraum vollständig abgedeckt ist. */
    public boolean fullyCovers(Instant from, Instant until) {
        return !startsAt.isAfter(from) && !endsAt.isBefore(until);
    }
}
