package de.ba.check.in.attendance.service;

import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.service.LectureEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AttendanceFinalizationJob {
    private final LectureRepository lectureRepository;
    private final LectureEligibilityService eligibilityService;
    private final AttendanceProjectionService projectionService;
    private final Clock clock;

    /** Schließt abgelaufene Termine ab. */
    @Scheduled(fixedDelayString = "${app.attendance.finalization-delay-ms:60000}")
    @Transactional
    public void finalizeEndedLectures() {
        Instant now = clock.instant();
        for (LectureEntity candidate : lectureRepository
                .findByAttendanceFinalizedAtIsNullAndEndsAtLessThanEqualOrderByEndsAt(now)) {
            LectureEntity lecture = lectureRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (lecture == null || lecture.getAttendanceFinalizedAt() != null) continue;
            eligibilityService.participants(lecture)
                    .forEach(student -> projectionService.project(student, lecture, true));
            lecture.markAttendanceFinalized(now);
        }
    }
}
