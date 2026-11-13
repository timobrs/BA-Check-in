package de.ba.check.in.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApplicationDtos {

    /** Erstellt eine neue Instanz. */
    private ApplicationDtos() {}

    public record Student(String studentId, String displayName, Long activatedSinceEpochMillis, String generatedActivationCode) {}
    public record Course(UUID id, String name) {}
    public record Lecturer(UUID id, String displayName, boolean active) {}
    public record Module(UUID id, UUID courseGroupId, String name, boolean ignored, UUID lecturerId) {}
    public record ModuleEnrollment(UUID studentId, UUID moduleId) {}
    public record Appointment(UUID id, String title, String room, String course, String protokollant,
                              Instant startAt, Instant endAt, UUID moduleId, boolean ignored,
                              UUID lecturerId, boolean lecturerOverride, boolean lecturerEditable) {}
    public record Absence(long fromEpochMillis, long untilEpochMillis, String reason, UUID id, String status) {}
    public record AbsenceReview(UUID id, Student student, long fromEpochMillis, long untilEpochMillis,
                                String reason, String status) {}
    public record Attendance(Student student, String status, Integer lateMinutes) {}
    public record AttendanceDay(Appointment appointment, List<Attendance> attendances, boolean attendancesValidated) {}
    public record AppointmentOverview(List<Appointment> appointments, UUID activeAppointmentId, AttendanceDay activeAttendanceDay) {}
    public record CheckInAvailability(String state, Appointment appointment, Instant opensAt,
                                      Instant validUntil, String attendanceStatus) {}
    public record StudentHomeData(Student student, List<Absence> absences,
                                  CheckInAvailability checkInAvailability, List<Appointment> appointments) {}
    public record LecturerHomeData(AppointmentOverview appointmentOverview) {}
    public record CourseDetail(Course course, List<Module> modules, List<Student> students,
                               List<Appointment> appointments, List<AttendanceDay> attendanceDays) {}
    public record SecretaryHomeData(List<CourseDetail> courseDetails, List<ModuleEnrollment> moduleEnrollments,
                                    List<AbsenceReview> absenceReviews, List<Lecturer> lecturers) {}
    public record LecturerUpdate(UUID lecturerId) {}
    public record LectureLecturerUpdate(boolean overrideModule, UUID lecturerId) {}
    public record AttendanceUpdate(String status) {}
    public record IgnoreUpdate(boolean ignored) {}
    public record ProtokollantUpdate(UUID studentId) {}
    public record AbsenceRequest(long fromEpochMillis, long untilEpochMillis) {}
    public record CalendarImportResult(int created, int updated, int archived, int skipped) {}
    public record NfcCheckInRequest(
            @NotNull UUID cardPublicId,
            @NotBlank @Pattern(regexp = "(?i)^[0-9a-f]{32}$") String encryptedPiccDataHex,
            @NotBlank @Pattern(regexp = "(?i)^[0-9a-f]{16}$") String sdmMacHex) {}
    public record NfcCheckInResult(
            String result, UUID appointmentId, String attendanceStatus,
            Long validUntilEpochMillis, String message) {}
    public record ModuleAssignmentRequest(@NotNull UUID moduleId) {}
    public record LectureModuleUpdate(@NotNull UUID moduleId) {}
    public record AbsenceReviewRequest(@NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status) {}
}
