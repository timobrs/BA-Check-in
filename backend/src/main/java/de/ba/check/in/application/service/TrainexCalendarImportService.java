package de.ba.check.in.application.service;

import de.ba.check.in.api.error.ApiErrorCode;
import de.ba.check.in.api.error.ApiException;
import de.ba.check.in.application.dto.ApplicationDtos.CalendarImportResult;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.domain.ModuleEntity;
import de.ba.check.in.modules.repository.ModuleRepository;
import de.ba.check.in.students.domain.CourseGroupEntity;
import de.ba.check.in.students.repository.CourseGroupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainexCalendarImportService {
    private static final Logger LOG = LoggerFactory.getLogger(TrainexCalendarImportService.class);
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private final LectureRepository lectureRepository;
    private final CourseGroupRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final Clock clock;

    /** Importiert Kalenderdaten. */
    @Transactional
    public CalendarImportResult importCalendar(String calendarKey, String calendarText) {
        ParseResult parsed = parse(calendarText);
        List<Event> events = parsed.events();
        Instant now = clock.instant();
        List<Event> currentEvents = events.stream()
                .filter(event -> event.end().isAfter(now))
                .toList();

        CourseGroupEntity calendarCourse = findCalendarCourse(events);

        int created = 0;
        int updated = 0;
        Set<String> importedIds = new HashSet<>();
        for (Event event : currentEvents) {
            String moduleTitle = baseModuleName(event.title());
            String normalizedModuleName = ModuleEntity.normalizeName(moduleTitle);
            ModuleEntity module = findModule(calendarCourse, moduleTitle, normalizedModuleName);
            Optional<LectureEntity> storedEvent = lectureRepository.findTrainexEvent(
                    calendarKey,
                    event.uid());
            LectureEntity lecture = storedEvent.orElseGet(
                    () -> LectureEntity.createTrainex(calendarCourse, calendarKey, event.uid()));
            lecture.setCourseGroup(calendarCourse);
            lecture.setModule(module);
            lecture.setTitle(event.title());
            lecture.setRoom(event.location());
            lecture.setStartsAt(event.start());
            lecture.setEndsAt(event.end());
            lecture.setActive(true);
            if (lecture.getStartsAt().isAfter(now) && lecture.getOpenedAt() == null
                    && lecture.getAttendanceFinalizedAt() == null) {
                lecture.useModuleLecturer();
            }
            lectureRepository.save(lecture);
            importedIds.add(event.uid());
            if (storedEvent.isPresent()) {
                updated++;
            } else {
                created++;
            }
        }

        int archived = 0;
        if (!events.isEmpty() || parsed.skipped() == 0) {
            for (LectureEntity lecture : lectureRepository.findActiveTrainex(calendarKey, now)) {
                if (!importedIds.contains(lecture.getExternalReference())) {
                    lecture.archive();
                    archived++;
                }
            }
        }
        return new CalendarImportResult(created, updated, archived, parsed.skipped());
    }

    /** Sucht den Kurs zum Kalender. */
    private CourseGroupEntity findCalendarCourse(List<Event> events) {
        if (events.isEmpty()) {
            return null;
        }

        List<String> courseNames = events.stream()
                .map(Event::courseName)
                .filter(name -> !name.isBlank())
                .toList();

        long distinctCourseNames = courseNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (distinctCourseNames != 1) {
            throw new CalendarImportValidationException(courseNames.isEmpty()
                    ? "Im Trainex-Kalender wurde kein Kursname gefunden."
                    : "Der Trainex-Kalender enthält Termine aus mehreren Kursen.");
        }
        String courseName = courseNames.getFirst();
        CourseGroupEntity course = courseRepository.findByNameIgnoreCase(courseName)
                .orElseGet(() -> courseRepository.save(CourseGroupEntity.create(courseName)));
        if (!course.isActive()) {
            throw new CalendarImportValidationException(
                    "Der Kurs '" + courseName + "' ist im Backend deaktiviert.");
        }
        return course;
    }

    /** Liest die Daten ein. */
    private ParseResult parse(String calendarText) {
        if (calendarText == null || calendarText.isBlank()
                || !calendarText.contains("BEGIN:VCALENDAR")
                || !calendarText.contains("END:VCALENDAR")) {
            throw invalidCalendar("Missing VCALENDAR envelope");
        }
        String unfolded = calendarText.replaceAll("\\r?\\n[ \\t]", "");
        List<Event> events = new ArrayList<>();
        int skipped = 0;
        String[] blocks = unfolded.split("BEGIN:VEVENT");
        for (int index = 1; index < blocks.length; index++) {
            String block = blocks[index];
            if (!block.contains("END:VEVENT")) {
                if (!block.isBlank()) {
                    skipped++;
                }
                continue;
            }
            try {
                Map<String, String> values = new HashMap<>();
                for (String line : block.substring(0, block.indexOf("END:VEVENT")).split("\r?\n")) {
                    int colon = line.indexOf(':');
                    if (colon <= 0) {
                        continue;
                    }
                    String fieldName = line.substring(0, colon)
                            .split(";", 2)[0]
                            .toUpperCase(Locale.ROOT);
                    values.put(
                            fieldName,
                            unescape(line.substring(colon + 1).trim()));
                }
                String title = required(values, "SUMMARY").split(" - ", 2)[0].trim();
                String location = values.getOrDefault("LOCATION", "").split(" - ", 2)[0].trim();
                String description = values.getOrDefault("DESCRIPTION", "");
                String courseName = between(description, "(", ")").trim();
                String uid = values.getOrDefault("UID", "").trim();
                if (uid.length() > 255
                        || title.length() > 255
                        || location.length() > 100
                        || courseName.length() > 100) {
                    throw new IllegalArgumentException("event field too long");
                }
                Instant start = parseDate(required(values, "DTSTART"));
                Instant end = parseDate(required(values, "DTEND"));
                if (end.equals(start)) {
                    end = start.plus(Duration.ofHours(1));
                } else if (end.isBefore(start)) {
                    throw new IllegalArgumentException("invalid range");
                }
                if (uid.isBlank()) {
                    uid = generatedReference(title, start);
                }
                events.add(new Event(uid, title, location, courseName, start, end));
            } catch (RuntimeException exception) {
                skipped++;
                LOG.warn("Skipping unsupported Trainex VEVENT: {}", exception.getMessage());
            }
        }
        Set<String> duplicateIds = events.stream()
                .collect(Collectors.groupingBy(Event::uid, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!duplicateIds.isEmpty()) {
            int duplicateCount = (int) events.stream()
                    .filter(event -> duplicateIds.contains(event.uid()))
                    .count();
            events.removeIf(event -> duplicateIds.contains(event.uid()));
            skipped += duplicateCount;
            LOG.warn("Skipping duplicate Trainex VEVENT identifiers");
        }
        return new ParseResult(events, skipped);
    }

    /** Liest das Datum. */
    private Instant parseDate(String value) {
        if (value.endsWith("Z")) {
            return Instant.from(
                    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").parse(value));
        }
        String normalized = value.length() >= 15 ? value.substring(0, 15) : value;
        return LocalDateTime.parse(
                        normalized,
                        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                .atZone(BERLIN)
                .toInstant();
    }

    /** Liest einen erforderlichen Wert. */
    private String required(Map<String, String> values, String key) {
        return Objects.requireNonNull(values.get(key), key);
    }

    /** Liest den Text zwischen zwei Markierungen. */
    private String between(String value, String start, String end) {
        int from = value.indexOf(start);
        int to = from < 0 ? -1 : value.indexOf(end, from + start.length());
        return from >= 0 && to > from ? value.substring(from + start.length(), to) : "";
    }

    /** Entfernt Escape-Zeichen. */
    private String unescape(String value) {
        return value.replace("\\n", " ").replace("\\,", ",").replace("\\;", ";");
    }

    /** Erzeugt eine Referenz für einen Kalendereintrag. */
    private String generatedReference(String title, Instant start) {
        String eventKey = title + "\u0000" + start;
        return "generated-" + UUID.nameUUIDFromBytes(eventKey.getBytes(StandardCharsets.UTF_8));
    }

    /** Sucht das passende Modul. */
    private ModuleEntity findModule(CourseGroupEntity course, String title, String normalizedName) {
        Optional<ModuleEntity> exactModule = moduleRepository.findByCourseGroupIdAndNormalizedName(
                course.getId(), normalizedName
        );
        if (exactModule.isPresent()) {
            return moduleRepository.findByIdForUpdate(exactModule.get().getId()).orElseThrow();
        }

        List<ModuleEntity> existingModules = moduleRepository.findByCourseGroupId(course.getId());
        for (ModuleEntity module : existingModules) {
            String baseName = baseModuleName(module.getName());
            String normalizedBaseName = ModuleEntity.normalizeName(baseName);

            if (normalizedBaseName.equals(normalizedName)) {
                module.setName(title);
                module.setNormalizedName(normalizedName);
                return moduleRepository.save(module);
            }
        }

        return moduleRepository.save(ModuleEntity.create(course, title));
    }

    /** Ermittelt den Modulnamen aus dem Titel. */
    private String baseModuleName(String title) {
        return title.replaceFirst("(?i)\\s*/\\s*online\\s+vorlesung.*$", "").trim();
    }

    /** Erstellt einen Fehler für ungültige Kalenderdaten. */
    private ApiException invalidCalendar(String message) {
        return new ApiException(ApiErrorCode.VALIDATION_FAILED, message);
    }

    private record Event(
            String uid,
            String title,
            String location,
            String courseName,
            Instant start,
            Instant end) {
    }

    private record ParseResult(List<Event> events, int skipped) {
    }
}
