package de.ba.check.in.application.service;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;
import de.ba.check.in.application.dto.ApplicationDtos.Absence;
import de.ba.check.in.application.dto.ApplicationDtos.AbsenceReview;
import de.ba.check.in.application.dto.ApplicationDtos.Appointment;
import de.ba.check.in.application.dto.ApplicationDtos.Lecturer;
import de.ba.check.in.application.dto.ApplicationDtos.AppointmentOverview;
import de.ba.check.in.application.dto.ApplicationDtos.Attendance;
import de.ba.check.in.application.dto.ApplicationDtos.AttendanceDay;
import de.ba.check.in.application.dto.ApplicationDtos.CheckInAvailability;
import de.ba.check.in.application.dto.ApplicationDtos.Course;
import de.ba.check.in.application.dto.ApplicationDtos.CourseDetail;
import de.ba.check.in.application.dto.ApplicationDtos.LecturerHomeData;
import de.ba.check.in.application.dto.ApplicationDtos.Module;
import de.ba.check.in.application.dto.ApplicationDtos.ModuleEnrollment;
import de.ba.check.in.application.dto.ApplicationDtos.SecretaryHomeData;
import de.ba.check.in.application.dto.ApplicationDtos.Student;
import de.ba.check.in.application.dto.ApplicationDtos.StudentHomeData;
import de.ba.check.in.attendance.domain.AttendanceRecordEntity;
import de.ba.check.in.attendance.domain.AttendanceStatus;
import de.ba.check.in.attendance.repository.AttendanceRecordRepository;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.devices.repository.DeviceRepository;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.domain.ModuleEntity;
import de.ba.check.in.absences.domain.AbsenceReportEntity;
import de.ba.check.in.absences.domain.AbsenceReportStatus;
import de.ba.check.in.absences.repository.AbsenceReportRepository;
import de.ba.check.in.attendance.service.AttendanceProjectionService;
import de.ba.check.in.modules.repository.ModuleRepository;
import de.ba.check.in.modules.repository.StudentModuleEnrollmentRepository;
import de.ba.check.in.modules.service.LectureEligibilityService;
import de.ba.check.in.students.domain.CourseGroupEntity;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.students.repository.CourseGroupRepository;
import de.ba.check.in.students.repository.StudentRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationFlowService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DeviceRepository deviceRepository;
    private final CourseGroupRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AbsenceReportRepository absenceRepository;
    private final ModuleRepository moduleRepository;
    private final StudentModuleEnrollmentRepository enrollmentRepository;
    private final LectureEligibilityService eligibilityService;
    private final AttendanceProjectionService projectionService;
    private final Clock clock;

    /** Lädt die Startdaten für Studierende. */
    @Transactional(readOnly = true)
    public StudentHomeData studentHome(UUID userId) {
        StudentEntity student = studentByUser(userId);
        Instant now = clock.instant();
        List<Appointment> appointments = eligibilityService.eligibleUpcoming(student, now)
                .stream()
                .map(this::toAppointment)
                .toList();
        List<Absence> absences = absenceRepository.findByStudentIdOrderByStartsAtDesc(student.getId())
                .stream()
                .filter(report -> report.getStatus() != AbsenceReportStatus.ARCHIVED)
                .map(report -> new Absence(
                        report.getStartsAt().toEpochMilli(),
                        report.getEndsAt().toEpochMilli(),
                        report.getReason(),
                        report.getId(),
                        report.getStatus().name()))
                .toList();
        Student profile = new Student(
                student.getMatriculationNumber(),
                displayName(student.getUser()),
                null,
                null);
        return new StudentHomeData(profile,
                absences, checkInAvailability(student, now), appointments);
    }

    /** Lädt die Startdaten für Dozenten. */
    @Transactional(readOnly = true)
    public LecturerHomeData lecturerHome(UUID userId) {
        List<LectureEntity> lectures = lectureRepository
                .findByLecturerUserIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(userId, clock.instant())
                .stream()
                .filter(LectureEntity::isRelevant)
                .filter(lecture -> lecture.getCourseGroup().isActive() && lecture.getModule().isActive())
                .toList();
        LectureEntity activeLecture = lectures.stream()
                .filter(lecture -> lecture.getOpenedAt() != null)
                .max(java.util.Comparator.comparing(LectureEntity::getOpenedAt))
                .orElse(null);
        List<Appointment> appointments = lectures.stream().map(this::toAppointment).toList();
        UUID activeId = activeLecture == null ? null : activeLecture.getId();
        AttendanceDay attendance = activeLecture == null ? null : toAttendanceDay(activeLecture);
        return new LecturerHomeData(new AppointmentOverview(appointments, activeId, attendance));
    }

    /** Lädt die Startdaten für das Sekretariat. */
    @Transactional(readOnly = true)
    public SecretaryHomeData secretaryHome() {
        Map<UUID, Long> activatedSinceByUserId = new HashMap<>();
        for (DeviceEntity device : deviceRepository.findActiveStudentDevices()) {
            if (device.getApprovedAt() != null) {
                activatedSinceByUserId.merge(device.getUser().getId(),
                        device.getApprovedAt().toEpochMilli(), Math::max);
            }
        }
        List<CourseDetail> courses = new ArrayList<>();
        for (CourseGroupEntity course : courseRepository.findByActiveTrueOrderByNameAsc()) {
            List<StudentEntity> students = studentRepository.findByCourseGroupIdOrderByUserLastNameAsc(course.getId());
            List<LectureEntity> lectures = lectureRepository
                    .findByCourseGroupIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(
                            course.getId(), Instant.EPOCH);
            List<Module> modules = moduleRepository.findByActiveTrueOrderByCourseGroupNameAscNameAsc().stream()
                    .filter(module -> module.getCourseGroup().getId().equals(course.getId()))
                    .map(module -> new Module(
                            module.getId(), course.getId(), module.getName(), module.isIgnored(),
                            module.getLecturerUser() == null ? null : module.getLecturerUser().getId()))
                    .toList();
            courses.add(new CourseDetail(
                    new Course(course.getId(), course.getName()),
                    modules,
                    students.stream().map(student -> toStudent(student,
                            activatedSinceByUserId.get(student.getUser().getId()))).toList(),
                    lectures.stream().map(this::toAppointment).toList(),
                    lectures.stream()
                            .filter(LectureEntity::isRelevant)
                            .map(this::toAttendanceDay)
                            .toList()));
        }
        List<ModuleEnrollment> enrollments = enrollmentRepository.findAll().stream()
                .map(enrollment -> new ModuleEnrollment(
                        enrollment.getStudent().getUser().getId(),
                        enrollment.getModule().getId()))
                .toList();
        List<AbsenceReview> reviews = absenceRepository
                .findByStatusAndEndsAtAfter(AbsenceReportStatus.SUBMITTED, clock.instant())
                .stream()
                .map(report -> new AbsenceReview(
                        report.getId(),
                        toStudent(report.getStudent()),
                        report.getStartsAt().toEpochMilli(),
                        report.getEndsAt().toEpochMilli(),
                        report.getReason(),
                        report.getStatus().name()))
                .toList();
        List<Lecturer> lecturers = userRepository.findByRole(UserRole.LECTURER).stream()
                .map(user -> new Lecturer(user.getId(), displayName(user), user.getStatus() == UserStatus.ACTIVE))
                .sorted(java.util.Comparator.comparing(Lecturer::displayName))
                .toList();
        return new SecretaryHomeData(courses, enrollments, reviews, lecturers);
    }

    /** Meldet eine Abwesenheit. */
    @Transactional
    public Absence reportAbsence(UUID userId, long startMillis, long endMillis) {
        StudentEntity student = studentByUser(userId);
        Instant startsAt = Instant.ofEpochMilli(startMillis);
        Instant endsAt = Instant.ofEpochMilli(endMillis);
        if (!endsAt.isAfter(startsAt)) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Absence end must be after start");
        }
        if (absenceRepository.findByStudentIdAndStartsAtLessThanAndEndsAtGreaterThan(
                student.getId(), endsAt, startsAt).stream()
                .anyMatch(report -> report.getStatus() == AbsenceReportStatus.SUBMITTED
                        || report.getStatus() == AbsenceReportStatus.APPROVED)) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Absence overlaps an existing absence");
        }
        AbsenceReportEntity report = absenceRepository.save(
                AbsenceReportEntity.create(student, startsAt, endsAt));
        refreshAttendanceForReport(report);
        return new Absence(
                startMillis,
                endMillis,
                null,
                report.getId(),
                report.getStatus().name());
    }

    /** Prüft die Abwesenheitsmeldung. */
    @Transactional
    public void reviewAbsence(UUID actorId, UUID absenceId, String decision) {
        AbsenceReportEntity report = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Absence not found"));
        AbsenceReportStatus status;
        try {
            status = AbsenceReportStatus.valueOf(decision);
        } catch (RuntimeException exception) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Review must be APPROVED or REJECTED");
        }
        report.review(status, requireUser(actorId), clock.instant());
        refreshAttendanceForReport(report);
    }

    /** Öffnet einen Termin. */
    @Transactional
    public AppointmentOverview openLecture(UUID lecturerId, UUID lectureId) {
        LectureEntity lecture = requireLecturerLecture(lecturerId, lectureId);
        lecture.open(clock.instant());
        return lecturerHome(lecturerId).appointmentOverview();
    }

    /** Aktualisiert den Ignorierstatus eines Termins. */
    @Transactional
    public void updateLectureIgnored(UUID lectureId, boolean ignored) {
        LectureEntity lecture = requireLecture(lectureId);
        lecture.setIgnored(ignored);
    }

    /** Aktualisiert den Ignorierstatus eines Moduls. */
    @Transactional
    public void updateModuleIgnored(UUID moduleId, boolean ignored) {
        ModuleEntity module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Module not found"));
        module.setIgnored(ignored);
    }

    /** Aktualisiert eine Anwesenheit. */
    @Transactional
    public AttendanceDay updateAttendance(
            UUID staffId,
            UUID lectureId,
            UUID studentUserId,
            String status,
            boolean asSecretary) {
        LectureEntity lecture = asSecretary
                ? requireLecture(lectureId)
                : requireLecturerLecture(staffId, lectureId);
        StudentEntity student = studentByUser(studentUserId);
        if (!eligibilityService.isEligible(student, lecture)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED);
        }
        if ("MISSING".equals(status)) {
            attendanceRepository.findByStudentIdAndLectureId(student.getId(), lectureId)
                    .ifPresent(attendanceRepository::delete);
            lecture.invalidateAttendances();
            return toAttendanceDay(lecture);
        }
        AttendanceRecordEntity record = attendanceRepository.findByStudentIdAndLectureId(student.getId(), lectureId)
                .orElseGet(() -> AttendanceRecordEntity.create(student, lecture));
        record.setStatus(toStatus(status));
        record.setManuallyChanged(true);
        record.setChangedBy(requireUser(staffId));
        attendanceRepository.save(record);
        lecture.invalidateAttendances();
        return toAttendanceDay(lecture);
    }

    /** Bestätigt die Anwesenheiten. */
    @Transactional
    public AttendanceDay validateAttendances(UUID staffId, UUID lectureId, boolean asSecretary) {
        LectureEntity lecture = asSecretary
                ? requireLecture(lectureId)
                : requireLecturerLecture(staffId, lectureId);
        lecture.validateAttendances(requireUser(staffId), clock.instant());
        return toAttendanceDay(lecture);
    }

    /** Wählt den Protokollanten aus. */
    @Transactional
    public AppointmentOverview selectProtokollant(UUID lecturerId, UUID lectureId, UUID studentUserId) {
        LectureEntity lecture = requireLecturerLecture(lecturerId, lectureId);
        StudentEntity student = studentByUser(studentUserId);
        if (!eligibilityService.isEligible(student, lecture)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED);
        }
        lecture.setProtokollant(student);
        return lecturerHome(lecturerId).appointmentOverview();
    }

    /** Wandelt den Wert in einen Anwesenheitstag um. */
    private AttendanceDay toAttendanceDay(LectureEntity lecture) {
        Map<UUID, AttendanceRecordEntity> recordsByStudent = new HashMap<>();
        attendanceRepository.findByLectureId(lecture.getId()).forEach(record ->
                recordsByStudent.put(record.getStudent().getId(), record));
        List<Attendance> attendances = eligibilityService.participants(lecture)
                .stream()
                .map(student -> attendanceFor(student, lecture, recordsByStudent.get(student.getId())))
                .toList();
        return new AttendanceDay(
                toAppointment(lecture),
                attendances,
                lecture.getAttendanceValidatedAt() != null);
    }

    /** Lädt die Anwesenheit. */
    private Attendance attendanceFor(
            StudentEntity student,
            LectureEntity lecture,
            AttendanceRecordEntity record) {
        String status = record == null ? "MISSING" : contractStatus(record.getStatus());
        Integer lateMinutes = null;
        if (record != null
                && record.getStatus() == AttendanceStatus.LATE
                && record.getCheckInTime() != null) {
            lateMinutes = Math.max(0, (int) Duration
                    .between(lecture.getStartsAt(), record.getCheckInTime())
                    .toMinutes());
        }
        return new Attendance(toStudent(student), status, lateMinutes);
    }

    /** Wandelt den Wert in einen Termin um. */
    private Appointment toAppointment(LectureEntity lecture) {
        String recorder = lecture.getProtokollant() == null
                ? ""
                : displayName(lecture.getProtokollant().getUser());
        return new Appointment(
                lecture.getId(),
                lecture.getTitle(),
                Objects.toString(lecture.getRoom(), ""),
                lecture.getCourseGroup().getName(),
                recorder,
                lecture.getStartsAt(),
                lecture.getEndsAt(),
                lecture.getModule().getId(),
                lecture.isIgnored(),
                lecture.getLecturerUser() == null || lecture.getLecturerUser().getStatus() != UserStatus.ACTIVE
                        ? null : lecture.getLecturerUser().getId(),
                lecture.isLecturerOverride(),
                lecture.getStartsAt().isAfter(clock.instant()) && lecture.getOpenedAt() == null
                        && lecture.getAttendanceFinalizedAt() == null);
    }

    /** Wandelt den Wert in einen Studierenden um. */
    private Student toStudent(StudentEntity student) {
        return toStudent(student, null);
    }

    /** Überträgt bei Bedarf den aktiven Gerätezugang in die Sekretariatsansicht. */
    private Student toStudent(StudentEntity student, Long activatedSinceEpochMillis) {
        return new Student(
                student.getUser().getId().toString(),
                displayName(student.getUser()),
                activatedSinceEpochMillis,
                null);
    }

    /** Gibt den Anzeigenamen zurück. */
    private String displayName(UserEntity user) {
        return (Objects.toString(user.getFirstName(), "") + " " + user.getLastName()).trim();
    }

    /** Lädt einen erforderlichen Nutzer. */
    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
    }

    /** Sucht den Studierenden zum Nutzer. */
    private StudentEntity studentByUser(UUID userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
    }

    /** Lädt einen erforderlichen Termin. */
    private LectureEntity requireLecture(UUID lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.VALIDATION_FAILED,
                        "Lecture not found"));
    }

    /** Lädt einen Termin des Dozenten. */
    private LectureEntity requireLecturerLecture(UUID lecturerId, UUID lectureId) {
        LectureEntity lecture = lectureRepository.findByIdForUpdate(lectureId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Lecture not found"));
        if (!lecture.isActive() || !lecture.isRelevant() || !lecture.getCourseGroup().isActive()
                || !lecture.getModule().isActive() || lecture.getLecturerUser() == null
                || !lecture.getLecturerUser().getId().equals(lecturerId)) {
            throw new ApiException(ApiErrorCode.ACCESS_DENIED);
        }
        return lecture;
    }

    /** Prüft die Verfügbarkeit des Check-ins. */
    private CheckInAvailability checkInAvailability(StudentEntity student, Instant now) {
        Optional<AttendanceRecordEntity> activeRecord = attendanceRepository
                .findFirstByStudentIdAndCheckInTimeIsNotNullAndLectureEndsAtAfterOrderByCheckInTimeDesc(
                        student.getId(), now);
        if (activeRecord.isPresent() && activeRecord.get().getLecture().isRelevant()) {
            AttendanceRecordEntity record = activeRecord.get();
            return new CheckInAvailability(
                    "ACTIVE",
                    toAppointment(record.getLecture()),
                    null,
                    record.getLecture().getEndsAt(),
                    contractStatus(record.getStatus()));
        }
        List<LectureEntity> available = eligibilityService.eligibleInCheckInWindow(student, now);
        if (available.size() > 1) {
            return new CheckInAvailability(
                    "OVERLAPPING_LECTURES", null, null, null, null);
        }
        if (available.size() == 1) {
            LectureEntity lecture = available.getFirst();
            if (projectionService.blocksNfc(student, lecture)) {
                return new CheckInAvailability(
                        "ABSENCE_BLOCKED",
                        toAppointment(lecture),
                        lecture.getStartsAt().minus(LectureEligibilityService.CHECK_IN_LEAD_TIME),
                        lecture.getEndsAt(),
                        null);
            }
            return new CheckInAvailability(
                    "AVAILABLE",
                    toAppointment(lecture),
                    lecture.getStartsAt().minus(LectureEligibilityService.CHECK_IN_LEAD_TIME),
                    lecture.getEndsAt(),
                    null);
        }
        LectureEntity nextLecture = eligibilityService.eligibleUpcoming(student, now)
                .stream()
                .findFirst()
                .orElse(null);
        return nextLecture == null
                ? new CheckInAvailability("NO_UPCOMING_LECTURE", null, null, null, null)
                : new CheckInAvailability(
                        "TOO_EARLY",
                        toAppointment(nextLecture),
                        nextLecture.getStartsAt().minus(LectureEligibilityService.CHECK_IN_LEAD_TIME),
                        nextLecture.getEndsAt(),
                        null);
    }

    /** Aktualisiert die Anwesenheit zur Meldung. */
    private void refreshAttendanceForReport(AbsenceReportEntity report) {
        Instant now = clock.instant();
        lectureRepository.findByActiveTrueAndStartsAtLessThanAndEndsAtGreaterThan(
                report.getEndsAt(), report.getStartsAt()).stream()
                .filter(LectureEntity::isRelevant)
                .filter(lecture -> eligibilityService.isEligible(report.getStudent(), lecture))
                .forEach(lecture -> projectionService.project(
                        report.getStudent(),
                        lecture,
                        !lecture.getEndsAt().isAfter(now)));
    }

    /** Wandelt den Wert in einen Status um. */
    private AttendanceStatus toStatus(String status) {
        try {
            return AttendanceStatus.valueOf(status);
        } catch (RuntimeException exception) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Unsupported attendance status");
        }
    }

    /** Ermittelt den Vertragsstatus. */
    private String contractStatus(AttendanceStatus status) {
        return status.name();
    }
}
