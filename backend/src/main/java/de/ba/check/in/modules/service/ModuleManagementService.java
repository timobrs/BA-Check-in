package de.ba.check.in.modules.service;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.domain.ModuleEntity;
import de.ba.check.in.modules.domain.StudentModuleEnrollmentEntity;
import de.ba.check.in.modules.repository.ModuleRepository;
import de.ba.check.in.modules.repository.StudentModuleEnrollmentRepository;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.students.repository.StudentRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModuleManagementService {
    private final ModuleRepository moduleRepository;
    private final StudentModuleEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final LectureRepository lectureRepository;
    private final LectureEligibilityService eligibilityService;
    private final Clock clock;

    /** Ordnet ein Modul zu. */
    @Transactional
    public void assign(UUID actorId, UUID studentUserId, UUID moduleId) {
        StudentEntity student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        ModuleEntity module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Module not found"));
        if (student.getCourseGroup().getId().equals(module.getCourseGroup().getId())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Course-group modules do not need an individual assignment");
        }
        if (enrollmentRepository.findByStudentIdAndModuleId(student.getId(), moduleId).isPresent()) {
            return;
        }

        Instant now = clock.instant();
        List<LectureEntity> addedLectures = lectureRepository
                .findByModuleIdAndActiveTrueAndEndsAtAfterOrderByStartsAt(moduleId, now);
        List<LectureEntity> existingLectures = eligibilityService.eligibleUpcoming(student, now);
        boolean overlaps = addedLectures.stream().anyMatch(addedLecture ->
                existingLectures.stream().anyMatch(existingLecture ->
                        addedLecture.getStartsAt().isBefore(existingLecture.getEndsAt())
                                && addedLecture.getEndsAt().isAfter(existingLecture.getStartsAt())));
        if (overlaps) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Module assignment creates overlapping lectures");
        }

        UserEntity actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        enrollmentRepository.save(StudentModuleEnrollmentEntity.create(student, module, actor, now));
    }

    /** Entfernt eine Modulzuordnung. */
    @Transactional
    public void remove(UUID studentUserId, UUID moduleId) {
        StudentEntity student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        enrollmentRepository.findByStudentIdAndModuleId(student.getId(), moduleId)
                .ifPresent(enrollmentRepository::delete);
    }

    /** Aktualisiert das Modul eines Termins. */
    @Transactional
    public void updateLectureModule(UUID lectureId, UUID moduleId) {
        LectureEntity lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Lecture not found"));
        ModuleEntity module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Module not found"));
        if (!lecture.getCourseGroup().getId().equals(module.getCourseGroup().getId())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "Lecture and module must belong to the same course group");
        }
        lecture.setModule(module);
        if (lecture.getStartsAt().isAfter(clock.instant()) && lecture.getOpenedAt() == null) {
            lecture.useModuleLecturer();
        }
    }

    /** Aktualisiert den Dozenten eines Moduls. */
    @Transactional
    public void updateModuleLecturer(UUID moduleId, UUID lecturerId) {
        ModuleEntity module = moduleRepository.findByIdForUpdate(moduleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Module not found"));
        UserEntity lecturer = lecturer(lecturerId);
        module.setLecturerUser(lecturer);
        for (LectureEntity lecture : lectureRepository.findFutureForModuleUpdate(moduleId, clock.instant())) {
            lecture.useModuleLecturer();
        }
    }

    /** Aktualisiert den Dozenten eines Termins. */
    @Transactional
    public void updateLectureLecturer(UUID lectureId, boolean overrideModule, UUID lecturerId) {
        LectureEntity lecture = lectureRepository.findByIdForUpdate(lectureId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.VALIDATION_FAILED, "Lecture not found"));
        if (!lecture.getStartsAt().isAfter(clock.instant()) || lecture.getOpenedAt() != null
                || lecture.getAttendanceFinalizedAt() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Only future, unopened lectures can be assigned");
        }
        if (!overrideModule && lecturerId != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "Module assignment must not include a lecturer ID");
        }
        lecture.setLecturerOverride(overrideModule);
        if (overrideModule) {
            lecture.setLecturerUser(lecturer(lecturerId));
        } else {
            lecture.useModuleLecturer();
        }
    }

    /** Lädt den Dozenten. */
    private UserEntity lecturer(UUID lecturerId) {
        if (lecturerId == null) {
            return null;
        }
        UserEntity lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.USER_NOT_FOUND));
        if (lecturer.getRole() != UserRole.LECTURER || lecturer.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.VALIDATION_FAILED, "An active lecturer account is required");
        }
        return lecturer;
    }
}
