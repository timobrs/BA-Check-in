package de.ba.check.in.attendance.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_attendance_records_student_lecture",
                        columnNames = {"student_id", "lecture_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecordEntity extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private LectureEntity lecture;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_event_id")
    private AttendanceEventEntity checkInEvent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_out_event_id")
    private AttendanceEventEntity checkOutEvent;

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_out_source", nullable = false, length = 50)
    private CheckOutSource checkOutSource = CheckOutSource.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "manually_changed", nullable = false)
    private boolean manuallyChanged = false;

    @Column(name = "manual_note", columnDefinition = "TEXT")
    private String manualNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private UserEntity changedBy;

    /** Erstellt einen Anwesenheitseintrag. */
    public static AttendanceRecordEntity create(StudentEntity student, LectureEntity lecture) {
        AttendanceRecordEntity attendanceRecord = new AttendanceRecordEntity();
        attendanceRecord.student = student;
        attendanceRecord.lecture = lecture;
        return attendanceRecord;
    }
}
