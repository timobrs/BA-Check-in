package de.ba.check.in.nfc.service;

import de.ba.check.in.application.dto.ApplicationDtos.NfcCheckInRequest;
import de.ba.check.in.application.dto.ApplicationDtos.NfcCheckInResult;
import de.ba.check.in.attendance.domain.AttendanceEventEntity;
import de.ba.check.in.attendance.domain.AttendanceRecordEntity;
import de.ba.check.in.attendance.domain.AttendanceStatus;
import de.ba.check.in.attendance.domain.ValidationStatus;
import de.ba.check.in.attendance.repository.AttendanceEventRepository;
import de.ba.check.in.attendance.repository.AttendanceRecordRepository;
import de.ba.check.in.attendance.service.AttendanceProjectionService;
import de.ba.check.in.auth.ratelimit.DatabaseSecurityRateLimiter;
import de.ba.check.in.auth.ratelimit.SecurityRateLimitPolicy;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.repository.DeviceCredentialRepository;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.service.LectureEligibilityService;
import de.ba.check.in.nfc.domain.NFCTagEntity;
import de.ba.check.in.nfc.repository.NfcTagRepository;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.students.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NfcCheckInService {
    private static final Duration PUNCTUAL_CHAIN_GAP = Duration.ofMinutes(2);

    private final StudentRepository studentRepository;
    private final DeviceCredentialRepository credentialRepository;
    private final NfcTagRepository tagRepository;
    private final LectureRepository lectureRepository;
    private final AttendanceEventRepository eventRepository;
    private final AttendanceRecordRepository recordRepository;
    private final LectureEligibilityService eligibilityService;
    private final AttendanceProjectionService attendanceProjectionService;
    private final DatabaseSecurityRateLimiter rateLimiter;
    private final NfcSdmVerifier sdmVerifier;
    private final NfcKeyProvider keyProvider;
    private final Clock clock;

    /** Verarbeitet einen NFC-Check-in. */
    @Transactional
    public NfcCheckInResult checkIn(UUID userId, UUID credentialId, NfcCheckInRequest request) {
        Instant now = clock.instant();
        StudentEntity student = studentRepository.findByUserId(userId).orElse(null);
        DeviceCredentialEntity credential = credentialRepository.findActiveCredential(credentialId).orElse(null);
        if (student == null || credential == null
                || !credential.getDevice().getUser().getId().equals(userId)) {
            return result("DEVICE_NOT_APPROVED", null, null, null,
                    "Das angemeldete Gerät ist nicht für NFC-Check-ins freigegeben.");
        }

        rateLimiter.checkSubjectOnly(SecurityRateLimitPolicy.NFC_DEVICE,
                student.getId() + ":" + credential.getDevice().getId());
        rateLimiter.checkSubjectOnly(SecurityRateLimitPolicy.NFC_CARD, request.cardPublicId().toString());

        String payloadHash = payloadHash(request);
        NFCTagEntity tag = tagRepository.findByPublicIdForUpdate(request.cardPublicId()).orElse(null);
        if (tag == null) {
            audit(student, credential, null, null, payloadHash, now,
                    ValidationStatus.UNKNOWN_TAG, "Unknown public card id");
            return result("INVALID_TAG", null, null, null, "Die NFC-Karte ist unbekannt.");
        }
        if (!tag.isActive()) {
            audit(student, credential, tag, null, payloadHash, now,
                    ValidationStatus.CARD_INACTIVE, "Inactive card");
            return result("CARD_INACTIVE", null, null, null, "Die NFC-Karte ist deaktiviert.");
        }

        NfcSdmVerifier.VerifiedTag verified;
        try {
            verified = sdmVerifier.verify(request.encryptedPiccDataHex(), request.sdmMacHex(),
                    keyProvider.keysFor(tag));
        } catch (IllegalArgumentException exception) {
            audit(student, credential, tag, null, payloadHash, now,
                    ValidationStatus.INVALID_SIGNATURE, "Invalid SDM cryptogram");
            return result("INVALID_TAG", null, null, null, "Die NFC-Karte konnte nicht authentifiziert werden.");
        }

        if (!sameTagId(verified.tagUidHex(), tag.getTagUid())) {
            audit(student, credential, tag, null, payloadHash, now,
                    ValidationStatus.INVALID_SIGNATURE, "Decrypted UID does not match registered UID");
            return result("INVALID_TAG", null, null, null, "Die NFC-Karte konnte nicht authentifiziert werden.");
        }
        if (verified.readCounter() <= tag.getLastReadCounter()) {
            ValidationStatus validationStatus = verified.readCounter() < tag.getLastReadCounter()
                    ? ValidationStatus.REPLAY_DETECTED : ValidationStatus.INVALID;
            audit(
                    student,
                    credential,
                    tag,
                    null,
                    payloadHash,
                    now,
                    validationStatus,
                    "Counter already consumed");
            return result("ALREADY_USED", null, null, null, "Diese NFC-Nachricht wurde bereits verwendet.");
        }

        tag.recordReadCounter(verified.readCounter(), now);
        tagRepository.save(tag);

        List<LectureEntity> candidates = eligibilityService.eligibleInCheckInWindow(student, now);
        if (candidates.isEmpty()) {
            audit(student, credential, tag, null, payloadHash, now,
                    ValidationStatus.NO_MATCHING_LECTURE, "No eligible lecture in check-in window");
            return result("NO_ELIGIBLE_LECTURE", null, null, null,
                    "Aktuell ist keine berechtigte Veranstaltung für den Check-in verfügbar.");
        }
        if (candidates.size() > 1) {
            audit(student, credential, tag, null, payloadHash, now,
                    ValidationStatus.OVERLAPPING_LECTURES, "Multiple eligible lectures in check-in window");
            return result("OVERLAPPING_LECTURES", null, null, null,
                    "Mehrere Veranstaltungen überschneiden sich. Bitte die Verwaltung kontaktieren.");
        }

        LectureEntity lecture = lectureRepository
                .findByIdForUpdate(candidates.getFirst().getId())
                .orElseThrow();
        if (now.isBefore(lecture.getStartsAt().minus(LectureEligibilityService.CHECK_IN_LEAD_TIME))
                || !now.isBefore(lecture.getEndsAt()) || !eligibilityService.isEligible(student, lecture)) {
            audit(student, credential, tag, lecture, payloadHash, now,
                    ValidationStatus.NO_MATCHING_LECTURE, "Lecture changed while processing check-in");
            return result("NO_ELIGIBLE_LECTURE", null, null, null,
                    "Aktuell ist keine berechtigte Veranstaltung für den Check-in verfügbar.");
        }
        if (attendanceProjectionService.blocksNfc(student, lecture)) {
            audit(student, credential, tag, lecture, payloadHash, now,
                    ValidationStatus.ABSENCE_ACTIVE, "Active absence report");
            return result("ABSENCE_ACTIVE", lecture.getId(), null, lecture.getEndsAt(),
                    "Für diese Veranstaltung ist eine Abwesenheit eingetragen.");
        }

        AttendanceRecordEntity record = recordRepository
                .findByStudentIdAndLectureId(student.getId(), lecture.getId()).orElse(null);
        if (record != null && (record.getCheckInTime() != null || record.isManuallyChanged())) {
            audit(student, credential, tag, lecture, payloadHash, now,
                    ValidationStatus.ALREADY_CHECKED_IN, "Attendance already exists");
            return result("ALREADY_CHECKED_IN", lecture.getId(), record.getStatus(), lecture.getEndsAt(),
                    "Für diese Veranstaltung besteht bereits ein Anwesenheitsstatus.");
        }

        AttendanceStatus status = determinePunctuality(lecture, now);
        AttendanceEventEntity event = AttendanceEventEntity.validCheckIn(
                student, lecture, credential.getDevice(), tag, payloadHash, now);
        eventRepository.save(event);
        if (record == null) {
            record = AttendanceRecordEntity.create(student, lecture);
        }
        record.setCheckInEvent(event);
        record.setCheckInTime(now);
        record.setStatus(status);
        recordRepository.save(record);
        return result("CHECKED_IN", lecture.getId(), status, lecture.getEndsAt(), null);
    }

    /** Ermittelt die Pünktlichkeit. */
    AttendanceStatus determinePunctuality(LectureEntity lecture, Instant now) {
        if (lecture.getLatePhaseStartedAt() != null) {
            return AttendanceStatus.LATE;
        }
        Instant lastPunctualTime = lecture.getLastPunctualCheckInAt() == null
                ? lecture.getStartsAt()
                : lecture.getLastPunctualCheckInAt();
        if (!now.isAfter(lastPunctualTime.plus(PUNCTUAL_CHAIN_GAP))) {
            if (now.isAfter(lecture.getStartsAt())) {
                lecture.recordPunctualCheckIn(now);
            }
            return AttendanceStatus.PRESENT;
        }
        lecture.startLatePhase(now);
        return AttendanceStatus.LATE;
    }

    /** Schreibt einen Audit-Eintrag. */
    private void audit(StudentEntity student, DeviceCredentialEntity credential, NFCTagEntity tag,
            LectureEntity lecture, String payloadHash, Instant now,
            ValidationStatus validationStatus, String reason) {
        eventRepository.save(AttendanceEventEntity.rejectedCheckIn(
                student,
                lecture,
                credential.getDevice(),
                tag,
                payloadHash,
                now,
                validationStatus,
                reason));
    }

    /** Vergleicht die Kartenkennungen. */
    private static boolean sameTagId(String actualTagId, String expectedTagId) {
        try {
            return MessageDigest.isEqual(
                    HexFormat.of().parseHex(actualTagId),
                    HexFormat.of().parseHex(expectedTagId));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Berechnet den Hash der Nutzdaten. */
    private static String payloadHash(NfcCheckInRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = request.cardPublicId() + "\n"
                    + request.encryptedPiccDataHex().toUpperCase() + "\n"
                    + request.sdmMacHex().toUpperCase();
            return HexFormat.of().formatHex(
                    digest.digest(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /** Erstellt das Ergebnis. */
    private static NfcCheckInResult result(
            String resultCode,
            UUID lectureId,
            AttendanceStatus attendanceStatus,
            Instant validUntil, String message) {
        return new NfcCheckInResult(
                resultCode,
                lectureId,
                attendanceStatus == null ? null : attendanceStatus.name(),
                validUntil == null ? null : validUntil.toEpochMilli(),
                message);
    }
}
