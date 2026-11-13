package de.ba.check.in.attendance.domain;

import de.ba.check.in.common.persistence.CreatedOnlyEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.nfc.domain.NFCTagEntity;
import de.ba.check.in.students.domain.StudentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attendance_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceEventEntity extends CreatedOnlyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private LectureEntity lecture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfc_tag_id")
    private NFCTagEntity nfcTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 50)
    private ValidationStatus validationStatus;

    @Column(name = "nfc_payload_hash", nullable = false, length = 255)
    private String nfcPayloadHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occuredAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /** Erstellt ein Ereignis für einen gültigen Check-in. */
    public static AttendanceEventEntity validCheckIn(StudentEntity student, LectureEntity lecture,
            DeviceEntity device, NFCTagEntity tag, String payloadHash, Instant now) {
        AttendanceEventEntity event = new AttendanceEventEntity();
        event.student = student;
        event.lecture = lecture;
        event.device = device;
        event.nfcTag = tag;
        event.validationStatus = ValidationStatus.VALID;
        event.nfcPayloadHash = payloadHash;
        event.occuredAt = now;
        return event;
    }

    /** Erstellt ein Ereignis für einen abgelehnten Check-in. */
    public static AttendanceEventEntity rejectedCheckIn(StudentEntity student, LectureEntity lecture,
            DeviceEntity device, NFCTagEntity tag, String payloadHash, Instant now,
            ValidationStatus status, String reason) {
        AttendanceEventEntity event = new AttendanceEventEntity();
        event.student = student;
        event.lecture = lecture;
        event.device = device;
        event.nfcTag = tag;
        event.validationStatus = status;
        event.nfcPayloadHash = payloadHash;
        event.occuredAt = now;
        event.failureReason = reason;
        return event;
    }
}
