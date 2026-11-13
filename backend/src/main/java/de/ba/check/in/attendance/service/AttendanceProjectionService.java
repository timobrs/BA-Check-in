package de.ba.check.in.attendance.service;

import de.ba.check.in.absences.domain.AbsenceReportEntity;
import de.ba.check.in.absences.domain.AbsenceReportStatus;
import de.ba.check.in.absences.repository.AbsenceReportRepository;
import de.ba.check.in.attendance.domain.AttendanceRecordEntity;
import de.ba.check.in.attendance.domain.AttendanceStatus;
import de.ba.check.in.attendance.repository.AttendanceRecordRepository;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.students.domain.StudentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceProjectionService {
    private final AbsenceReportRepository absenceRepository;
    private final AttendanceRecordRepository attendanceRepository;

    /** Prüft, ob NFC-Check-in gesperrt ist. */
    @Transactional(readOnly = true)
    public boolean blocksNfc(StudentEntity student, LectureEntity lecture) {
        return relevantReports(student, lecture).stream().anyMatch(report ->
                report.getStatus() == AbsenceReportStatus.SUBMITTED
                        || report.getStatus() == AbsenceReportStatus.APPROVED);
    }

    /** Berechnet den Anwesenheitsstand. */
    @Transactional
    public AttendanceRecordEntity project(
            StudentEntity student,
            LectureEntity lecture,
            boolean createAbsent) {
        AttendanceRecordEntity record = attendanceRepository
                .findByStudentIdAndLectureId(student.getId(), lecture.getId())
                .orElse(null);
        if (record != null && record.isManuallyChanged()) {
            return record;
        }

        List<AbsenceReportEntity> reports = relevantReports(student, lecture);
        boolean submitted = reports.stream().anyMatch(report -> report.getStatus() == AbsenceReportStatus.SUBMITTED);
        boolean approvedFull = reports.stream().anyMatch(report ->
                report.getStatus() == AbsenceReportStatus.APPROVED
                        && report.fullyCovers(lecture.getStartsAt(), lecture.getEndsAt()));
        boolean approvedPartial = reports.stream().anyMatch(report ->
                report.getStatus() == AbsenceReportStatus.APPROVED
                        && !report.fullyCovers(lecture.getStartsAt(), lecture.getEndsAt()));

        AttendanceStatus projected = null;
        if (submitted) {
            projected = AttendanceStatus.ABSENT;
        } else if (approvedFull) {
            projected = AttendanceStatus.EXCUSED;
        } else if (record != null && record.getCheckInTime() != null) {
            projected = approvedPartial
                    ? AttendanceStatus.PARTIALLY
                    : automaticCheckInStatus(record, lecture);
        } else if (approvedPartial || createAbsent) {
            projected = AttendanceStatus.ABSENT;
        }

        if (projected == null) {
            if (record != null) {
                attendanceRepository.delete(record);
            }
            return null;
        }
        if (record == null) {
            record = AttendanceRecordEntity.create(student, lecture);
        }
        record.setStatus(projected);
        return attendanceRepository.save(record);
    }

    /** Ermittelt den automatischen Check-in-Status. */
    public AttendanceStatus automaticCheckInStatus(AttendanceRecordEntity record, LectureEntity lecture) {
        if (record.getCheckInTime() == null) {
            return AttendanceStatus.ABSENT;
        }
        return lecture.getLatePhaseStartedAt() != null
                && !record.getCheckInTime().isBefore(lecture.getLatePhaseStartedAt())
                ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }

    /** Sucht relevante Abwesenheitsmeldungen. */
    private List<AbsenceReportEntity> relevantReports(StudentEntity student, LectureEntity lecture) {
        return absenceRepository.findByStudentIdAndStartsAtLessThanAndEndsAtGreaterThan(
                        student.getId(),
                        lecture.getEndsAt(),
                        lecture.getStartsAt())
                .stream()
                .filter(report -> report.getStatus() == AbsenceReportStatus.SUBMITTED
                        || report.getStatus() == AbsenceReportStatus.APPROVED)
                .toList();
    }
}
