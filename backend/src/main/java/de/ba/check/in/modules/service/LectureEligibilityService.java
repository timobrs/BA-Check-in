package de.ba.check.in.modules.service;

import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.repository.StudentModuleEnrollmentRepository;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.students.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LectureEligibilityService {
    public static final Duration CHECK_IN_LEAD_TIME = Duration.ofMinutes(30);

    private final LectureRepository lectureRepository;
    private final StudentRepository studentRepository;
    private final StudentModuleEnrollmentRepository enrollmentRepository;

    /** Sucht passende Termine im Check-in-Zeitraum. */
    @Transactional(readOnly = true)
    public List<LectureEntity> eligibleInCheckInWindow(StudentEntity student, Instant now) {
        return lectureRepository.findEligibleInCheckInWindow(
                student.getId(),
                student.getCourseGroup().getId(),
                now,
                now.plus(CHECK_IN_LEAD_TIME)).stream()
                .filter(LectureEntity::isRelevant)
                .toList();
    }

    /** Sucht bevorstehende passende Termine. */
    @Transactional(readOnly = true)
    public List<LectureEntity> eligibleUpcoming(StudentEntity student, Instant now) {
        return lectureRepository.findEligibleUpcoming(
                student.getId(),
                student.getCourseGroup().getId(),
                now).stream()
                .filter(LectureEntity::isRelevant)
                .toList();
    }

    /** Prüft die Berechtigung. */
    @Transactional(readOnly = true)
    public boolean isEligible(StudentEntity student, LectureEntity lecture) {
        return lecture.isRelevant()
                && (student.getCourseGroup().getId().equals(lecture.getCourseGroup().getId())
                || enrollmentRepository.findByStudentIdAndModuleId(
                        student.getId(),
                        lecture.getModule().getId()).isPresent());
    }

    /** Lädt die Teilnehmenden. */
    @Transactional(readOnly = true)
    public List<StudentEntity> participants(LectureEntity lecture) {
        LinkedHashMap<UUID, StudentEntity> participants = new LinkedHashMap<>();
        studentRepository.findByCourseGroupIdOrderByUserLastNameAsc(lecture.getCourseGroup().getId())
                .forEach(student -> participants.put(student.getId(), student));
        List<UUID> enrolledIds = enrollmentRepository.findStudentIdsByModuleId(lecture.getModule().getId());
        studentRepository.findByIdIn(enrolledIds)
                .forEach(student -> participants.put(student.getId(), student));
        return participants.values().stream()
                .sorted(Comparator.comparing(
                        student -> student.getUser().getLastName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
