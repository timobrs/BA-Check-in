package de.ba.check.in.admin;

import de.ba.check.in.auth.activation.domain.ActivationMode;
import de.ba.check.in.auth.activation.dto.CreatedActivationCodeResponse;
import de.ba.check.in.auth.activation.service.ActivationCodeService;
import de.ba.check.in.devices.management.service.DeviceManagementService;
import de.ba.check.in.devices.repository.DeviceRepository;
import de.ba.check.in.lectures.domain.LectureEntity;
import de.ba.check.in.lectures.repository.LectureRepository;
import de.ba.check.in.modules.domain.ModuleEntity;
import de.ba.check.in.modules.repository.ModuleRepository;
import de.ba.check.in.nfc.domain.NFCTagEntity;
import de.ba.check.in.nfc.repository.NfcTagRepository;
import de.ba.check.in.students.domain.CourseGroupEntity;
import de.ba.check.in.students.domain.StudentEntity;
import de.ba.check.in.students.repository.CourseGroupRepository;
import de.ba.check.in.students.repository.StudentRepository;
import de.ba.check.in.users.domain.UserEntity;
import de.ba.check.in.users.domain.UserRole;
import de.ba.check.in.users.domain.UserStatus;
import de.ba.check.in.users.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Component
@Profile("admin")
public class AdminCommandRunner implements ApplicationRunner {
    private final UserRepository users;
    private final StudentRepository students;
    private final CourseGroupRepository courses;
    private final DeviceRepository devices;
    private final NfcTagRepository cards;
    private final LectureRepository lectures;
    private final ModuleRepository modules;
    private final String nfcKeyVersion;
    private final DeviceManagementService deviceService;
    private final ActivationCodeService activationCodes;
    private final TransactionTemplate transactions;
    private final ConfigurableApplicationContext context;

    /** Erstellt den Startablauf. */
    public AdminCommandRunner(UserRepository users,
            StudentRepository students,
            CourseGroupRepository courses,
            DeviceRepository devices,
            NfcTagRepository cards,
            LectureRepository lectures,
            ModuleRepository modules,
            @Value("${app.nfc.key-version:v1}") String nfcKeyVersion,
            DeviceManagementService deviceService,
            ActivationCodeService activationCodes,
            TransactionTemplate transactions,
            ConfigurableApplicationContext context) {
        this.users = users;
        this.students = students;
        this.courses = courses;
        this.devices = devices;
        this.cards = cards;
        this.lectures = lectures;
        this.modules = modules;
        this.nfcKeyVersion = nfcKeyVersion;
        this.deviceService = deviceService;
        this.activationCodes = activationCodes;
        this.transactions = transactions;
        this.context = context;
    }

    /** Führt den Admin-Befehl aus. */
    @Override
    public void run(@NonNull ApplicationArguments arguments) {
        List<String> output = transactions.execute(status -> execute(arguments.getNonOptionArgs()));
        if (output != null) {
            output.forEach(System.out::println);
        }
        System.exit(SpringApplication.exit(context));
    }

    /** Führt den Befehl aus. */
    List<String> execute(List<String> args) {
        if (args.size() < 2) {
            throw usage();
        }
        return switch (args.getFirst()) {
            case "secretary" -> secretary(args);
            case "lecturer" -> lecturer(args);
            case "student" -> student(args);
            case "course" -> course(args);
            case "device" -> device(args);
            case "card" -> card(args);
            case "lecture" -> lecture(args);
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> secretary(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 2, "ba-admin secretary list");
                yield output(users.findByRole(UserRole.SECRETARY).stream()
                        .sorted(Comparator.comparing(UserEntity::getEmail))
                        .map(user -> "%s | %s | %s".formatted(user.getEmail(), user.getStatus(), name(user)))
                        .toList());
            }
            case "create" -> {
                requireSize(args, 5, "ba-admin secretary create <first-name> <last-name> <email>");
                String email = email(args.get(4));
                ensureEmailIsFree(email);
                UserEntity secretary = users.save(UserEntity.create(
                        required(args.get(2), "first name", 100),
                        required(args.get(3), "last name", 100),
                        email,
                        UserRole.SECRETARY));
                yield activationOutput(activationCodes.createActivationCode(
                        secretary.getId(), secretary.getId(), ActivationMode.ADD_DEVICE));
            }
            case "update" -> {
                requireSize(args, 6,
                        "ba-admin secretary update <email> <first-name> <last-name> <new-email>");
                UserEntity secretary = user(args.get(2));
                requireRole(secretary, UserRole.SECRETARY);
                String newEmail = email(args.get(5));
                ensureEmailIsFree(newEmail, secretary.getId());
                secretary.setFirstName(required(args.get(3), "first name", 100));
                secretary.setLastName(required(args.get(4), "last name", 100));
                secretary.setEmail(newEmail);
                yield List.of("SECRETARY_UPDATED=" + newEmail);
            }
            case "code" -> {
                requireSize(args, 3, "ba-admin secretary code <email>");
                UserEntity secretary = activeUser(args.get(2), UserRole.SECRETARY);
                deviceService.revokeActiveForUser(
                        secretary.getId(), secretary.getId(), "Replaced through admin command");
                yield activationOutput(activationCodes.createActivationCode(
                        secretary.getId(), secretary.getId(), ActivationMode.ADD_DEVICE));
            }
            case "disable" -> {
                requireSize(args, 3, "ba-admin secretary disable <email>");
                UserEntity secretary = activeUser(args.get(2), UserRole.SECRETARY);
                deviceService.revokeActiveForUser(
                        secretary.getId(), secretary.getId(), "Secretary account disabled");
                activationCodes.revokeOpenCodes(secretary.getId());
                secretary.setStatus(UserStatus.INACTIVE);
                yield List.of("SECRETARY_DISABLED=" + secretary.getEmail());
            }
            case "enable" -> {
                requireSize(args, 3, "ba-admin secretary enable <email>");
                UserEntity secretary = user(args.get(2));
                requireRole(secretary, UserRole.SECRETARY);
                secretary.setStatus(UserStatus.ACTIVE);
                yield List.of("SECRETARY_ENABLED=" + secretary.getEmail());
            }
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> lecturer(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 2, "ba-admin lecturer list");
                yield output(users.findByRole(UserRole.LECTURER).stream()
                        .sorted(Comparator.comparing(UserEntity::getEmail))
                        .map(user -> "%s | %s | %s".formatted(user.getEmail(), user.getStatus(), name(user)))
                        .toList());
            }
            case "create" -> {
                requireSize(args, 5, "ba-admin lecturer create <first-name> <last-name> <email>");
                String email = email(args.get(4));
                ensureEmailIsFree(email);
                users.save(UserEntity.create(required(args.get(2), "first name", 100),
                        required(args.get(3), "last name", 100), email, UserRole.LECTURER));
                yield List.of("LECTURER_CREATED=" + email);
            }
            case "update" -> {
                requireSize(args, 6, "ba-admin lecturer update <email> <first-name> <last-name> <new-email>");
                UserEntity lecturer = user(args.get(2));
                requireRole(lecturer, UserRole.LECTURER);
                String newEmail = email(args.get(5));
                ensureEmailIsFree(newEmail, lecturer.getId());
                lecturer.setFirstName(required(args.get(3), "first name", 100));
                lecturer.setLastName(required(args.get(4), "last name", 100));
                lecturer.setEmail(newEmail);
                yield List.of("LECTURER_UPDATED=" + newEmail);
            }
            case "code" -> {
                requireSize(args, 4, "ba-admin lecturer code <lecturer-email> <secretary-email>");
                UserEntity lecturer = activeUser(args.get(2), UserRole.LECTURER);
                UserEntity secretary = activeUser(args.get(3), UserRole.SECRETARY);
                yield activationOutput(activationCodes.createAutomaticActivationCode(lecturer.getId(), secretary.getId()));
            }
            case "disable" -> {
                requireSize(args, 4, "ba-admin lecturer disable <lecturer-email> <secretary-email>");
                UserEntity lecturer = activeUser(args.get(2), UserRole.LECTURER);
                UserEntity secretary = activeUser(args.get(3), UserRole.SECRETARY);
                deviceService.revokeActiveForUser(lecturer.getId(), secretary.getId(), "Lecturer account disabled");
                activationCodes.revokeOpenCodes(lecturer.getId());
                lecturer.setStatus(UserStatus.INACTIVE);
                yield List.of("LECTURER_DISABLED=" + lecturer.getEmail());
            }
            case "enable" -> {
                requireSize(args, 3, "ba-admin lecturer enable <email>");
                UserEntity lecturer = user(args.get(2));
                requireRole(lecturer, UserRole.LECTURER);
                lecturer.setStatus(UserStatus.ACTIVE);
                yield List.of("LECTURER_ENABLED=" + lecturer.getEmail());
            }
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> student(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                if (args.size() > 3) {
                    throw new IllegalArgumentException("Usage: ba-admin student list [course]");
                }
                List<StudentEntity> found = args.size() == 3
                        ? students.findByCourseGroupIdOrderByUserLastNameAsc(course(args.get(2)).getId())
                        : students.findAll();
                yield output(found.stream()
                        .sorted(Comparator.comparing(student -> student.getUser().getEmail()))
                        .map(student -> "%s | %s | %s | %s".formatted(
                                student.getMatriculationNumber(),
                                student.getUser().getEmail(),
                                student.getUser().getStatus(),
                                student.getCourseGroup().getName()))
                        .toList());
            }
            case "create" -> {
                requireSize(args, 7,
                        "ba-admin student create <course> <matriculation> <first-name> <last-name> <email>");
                CourseGroupEntity course = course(args.get(2));
                String matriculation = required(args.get(3), "matriculation number", 50);
                String email = email(args.get(6));
                ensureEmailIsFree(email);
                if (students.findByMatriculationNumberIgnoreCase(matriculation).isPresent()) {
                    throw new IllegalArgumentException("Matriculation number already exists");
                }
                UserEntity user = users.save(UserEntity.create(
                        required(args.get(4), "first name", 100),
                        required(args.get(5), "last name", 100),
                        email,
                        UserRole.STUDENT));
                students.save(StudentEntity.create(user, matriculation, course));
                yield List.of("STUDENT_CREATED=" + email);
            }
            case "update" -> {
                requireSize(args, 7,
                        "ba-admin student update <email> <matriculation> <first-name> <last-name> <new-email>");
                StudentEntity student = student(args.get(2));
                String matriculation = required(args.get(3), "matriculation number", 50);
                String newEmail = email(args.get(6));
                ensureEmailIsFree(newEmail, student.getUser().getId());
                students.findByMatriculationNumberIgnoreCase(matriculation)
                        .filter(existing -> !Objects.equals(existing.getId(), student.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Matriculation number already exists");
                        });
                student.setMatriculationNumber(matriculation);
                student.getUser().setFirstName(required(args.get(4), "first name", 100));
                student.getUser().setLastName(required(args.get(5), "last name", 100));
                student.getUser().setEmail(newEmail);
                yield List.of("STUDENT_UPDATED=" + newEmail);
            }
            case "code" -> {
                requireSize(args, 4, "ba-admin student code <student-email> <secretary-email>");
                StudentEntity student = student(args.get(2));
                UserEntity secretary = activeUser(args.get(3), UserRole.SECRETARY);
                if (student.getUser().getStatus() != UserStatus.ACTIVE) {
                    throw new IllegalArgumentException("Student account is not active");
                }
                yield activationOutput(activationCodes.createAutomaticActivationCode(
                        student.getUser().getId(), secretary.getId()));
            }
            case "course" -> {
                requireSize(args, 4, "ba-admin student course <student-email> <course>");
                StudentEntity student = student(args.get(2));
                student.setCourseGroup(course(args.get(3)));
                yield List.of("STUDENT_COURSE_UPDATED=" + student.getUser().getEmail());
            }
            case "disable" -> {
                requireSize(args, 4, "ba-admin student disable <student-email> <secretary-email>");
                StudentEntity student = student(args.get(2));
                UserEntity secretary = activeUser(args.get(3), UserRole.SECRETARY);
                deviceService.revokeActiveForUser(
                        student.getUser().getId(), secretary.getId(), "Student account disabled");
                activationCodes.revokeOpenCodes(student.getUser().getId());
                student.getUser().setStatus(UserStatus.INACTIVE);
                yield List.of("STUDENT_DISABLED=" + student.getUser().getEmail());
            }
            case "enable" -> {
                requireSize(args, 3, "ba-admin student enable <student-email>");
                StudentEntity student = student(args.get(2));
                student.getUser().setStatus(UserStatus.ACTIVE);
                yield List.of("STUDENT_ENABLED=" + student.getUser().getEmail());
            }
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> course(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 2, "ba-admin course list");
                yield output(courses.findAll().stream()
                        .sorted(Comparator.comparing(CourseGroupEntity::getName))
                        .map(course -> "%s | active=%s".formatted(course.getName(), course.isActive()))
                        .toList());
            }
            case "create" -> {
                if (args.size() != 3) {
                    throw new IllegalArgumentException("Usage: ba-admin course create <name>");
                }
                String name = required(args.get(2), "course name", 100);
                if (courses.findByNameIgnoreCase(name).isPresent()) {
                    throw new IllegalArgumentException("Course already exists");
                }
                courses.save(CourseGroupEntity.create(name));
                yield List.of("COURSE_CREATED=" + name);
            }
            case "update" -> {
                if (args.size() != 4) {
                    throw new IllegalArgumentException("Usage: ba-admin course update <name> <new-name>");
                }
                CourseGroupEntity course = existingCourse(args.get(2));
                String newName = required(args.get(3), "course name", 100);
                courses.findByNameIgnoreCase(newName)
                        .filter(existing -> !Objects.equals(existing.getId(), course.getId()))
                        .ifPresent(existing -> {
                            throw new IllegalArgumentException("Course already exists");
                        });
                course.setName(newName);
                yield List.of("COURSE_UPDATED=" + newName);
            }
            case "enable", "disable" -> {
                requireSize(args, 3, "ba-admin course <enable|disable> <name>");
                CourseGroupEntity course = existingCourse(args.get(2));
                course.setActive("enable".equals(args.get(1)));
                yield List.of("COURSE_ACTIVE=%s:%s".formatted(course.getName(), course.isActive()));
            }
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> lecture(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 3, "ba-admin lecture list <course>");
                yield output(lectures.findByCourseGroupIdOrderByStartsAt(existingCourse(args.get(2)).getId())
                        .stream().map(lecture -> "%s | %s | %s | %s | %s | active=%s | %s".formatted(
                                lecture.getId(), lecture.getTitle(), lecture.getModule().getName(),
                                lecture.getStartsAt(), lecture.getEndsAt(), lecture.isActive(), lecture.getSource()))
                        .toList());
            }
            case "create", "update" -> {
                boolean create = "create".equals(args.get(1));
                requireSize(args, 8, "ba-admin lecture " + args.get(1)
                        + " <" + (create ? "course" : "lecture-id") + "> <module> <title> <start> <end> <room|->");
                LectureEntity lecture = create ? LectureEntity.createManual(course(args.get(2)))
                        : existingLecture(args.get(2));
                if (!create) {
                    requireEditable(lecture);
                }
                Instant start = lectureTime(args.get(5));
                Instant end = lectureTime(args.get(6));
                if (!end.isAfter(start)) {
                    throw new IllegalArgumentException("End must be after start");
                }
                String moduleName = required(args.get(3), "module name", 255);
                String normalized = ModuleEntity.normalizeName(moduleName);
                if (normalized.isEmpty()) {
                    throw new IllegalArgumentException("Module name must contain letters or numbers");
                }
                String title = required(args.get(4), "lecture title", 255);
                String room = "-".equals(args.get(7)) ? null : required(args.get(7), "room", 100);
                ModuleEntity module = modules.findByCourseGroupIdAndNormalizedName(
                        lecture.getCourseGroup().getId(), normalized)
                        .orElseGet(() -> modules.save(ModuleEntity.create(lecture.getCourseGroup(), moduleName)));
                if (!module.isActive()) {
                    throw new IllegalArgumentException("Module is inactive");
                }
                lecture.setModule(module);
                lecture.setTitle(title);
                lecture.setStartsAt(start);
                lecture.setEndsAt(end);
                lecture.setRoom(room);
                if (start.isAfter(Instant.now()) && lecture.getOpenedAt() == null) {
                    lecture.useModuleLecturer();
                }
                lectures.save(lecture);
                yield List.of("LECTURE_ID=" + lecture.getId(),
                        create ? "LECTURE_CREATED=" + title : "LECTURE_UPDATED=" + title);
            }
            case "delete" -> {
                requireSize(args, 3, "ba-admin lecture delete <lecture-id>");
                LectureEntity lecture = existingLecture(args.get(2));
                requireDeletable(lecture);
                lectures.delete(lecture);
                yield List.of("LECTURE_DELETED=" + lecture.getId());
            }
            default -> throw usage();
        };
    }

    /** Lädt einen vorhandenen Termin. */
    private LectureEntity existingLecture(String value) {
        UUID id;
        try {
            id = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid lecture ID");
        }
        return lectures.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
    }

    /** Prüft, ob der Eintrag bearbeitet werden darf. */
    private void requireEditable(LectureEntity lecture) {
        if (hasEditBlockers(lecture)) {
            throw new IllegalArgumentException("Opened or finalized lectures and lectures with attendance cannot be changed");
        }
    }

    /** Erlaubt das Löschen aller vergangenen Termine. */
    private void requireDeletable(LectureEntity lecture) {
        if (lecture.getEndsAt().isAfter(Instant.now()) && hasEditBlockers(lecture)) {
            throw new IllegalArgumentException("Opened or finalized lectures and lectures with attendance cannot be deleted");
        }
    }

    /** Prüft, ob ein Termin bereits genutzt wurde. */
    private boolean hasEditBlockers(LectureEntity lecture) {
        return lecture.getOpenedAt() != null || lecture.getAttendanceFinalizedAt() != null
                || lectures.hasAttendance(lecture.getId());
    }

    /** Liest die Terminzeit. */
    private Instant lectureTime(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Expected date/time with offset, e.g. 2026-09-14T18:00:00+02:00");
        }
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> device(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 3, "ba-admin device list <user-email>");
                UserEntity user = user(args.get(2));
                yield output(devices.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                        .map(device -> "%s | %s | %s | %s".formatted(
                                device.getId(), device.getDeviceStatus(), device.getPlatform(), device.getDeviceName()))
                        .toList());
            }
            case "revoke" -> {
                requireSize(args, 4, "ba-admin device revoke <device-id> <secretary-email>");
                UserEntity secretary = activeUser(args.get(3), UserRole.SECRETARY);
                UUID deviceId;
                try {
                    deviceId = UUID.fromString(args.get(2));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Invalid device ID");
                }
                deviceService.revoke(deviceId, secretary.getId(), "Revoked through admin command");
                yield List.of("DEVICE_REVOKED=" + deviceId);
            }
            default -> throw usage();
        };
    }

    /** Verarbeitet den Admin-Befehl. */
    private List<String> card(List<String> args) {
        return switch (args.get(1)) {
            case "list" -> {
                requireSize(args, 2, "ba-admin card list");
                yield output(cards.findAll().stream()
                        .sorted(Comparator.comparing(NFCTagEntity::getTagUid))
                        .map(tag -> "%s | %s | active=%s | %s".formatted(
                                tag.getTagUid(), tag.getDisplayName(), tag.isActive(), tag.getPublicId()))
                        .toList());
            }
            case "show" -> {
                requireSize(args, 3, "ba-admin card show <uid|public-id>");
                yield cardOutput(existingCard(args.get(2)));
            }
            case "create" -> {
                requireSize(args, 4, "ba-admin card create <uid> <name>");
                String uid = required(args.get(2), "card UID", 14).toUpperCase(Locale.ROOT);
                if (!uid.matches("[0-9A-F]{14}")) {
                    throw new IllegalArgumentException("Card UID must contain exactly 14 hex characters");
                }
                if (cards.existsByTagUidIgnoreCase(uid)) {
                    throw new IllegalArgumentException("Card already exists");
                }
                NFCTagEntity tag = cards.save(NFCTagEntity.create(
                        uid, required(args.get(3), "card name", 255), nfcKeyVersion));
                yield cardOutput(tag);
            }
            case "update" -> {
                requireSize(args, 4, "ba-admin card update <uid|public-id> <name>");
                NFCTagEntity tag = existingCard(args.get(2));
                tag.setDisplayName(required(args.get(3), "card name", 255));
                yield cardOutput(tag);
            }
            case "enable", "disable" -> {
                requireSize(args, 3, "ba-admin card <enable|disable> <uid|public-id>");
                NFCTagEntity tag = existingCard(args.get(2));
                tag.setActive("enable".equals(args.get(1)));
                yield List.of("CARD_ACTIVE=%s:%s".formatted(tag.getTagUid(), tag.isActive()));
            }
            default -> throw usage();
        };
    }

    /** Lädt eine vorhandene NFC-Karte. */
    private NFCTagEntity existingCard(String value) {
        String id = required(value, "card UID or public ID", 36);
        if (id.matches("(?i)[0-9a-f]{14}")) {
            return cards.findByUidForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        }
        UUID publicId;
        try {
            publicId = UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Expected a 14-character hex UID or a public card UUID");
        }
        return cards.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    /** Erstellt die Ausgabe zur NFC-Karte. */
    private List<String> cardOutput(NFCTagEntity tag) {
        return List.of(
                "CARD_UID=" + tag.getTagUid(),
                "CARD_PUBLIC_ID=" + tag.getPublicId(),
                "CARD_NAME=" + tag.getDisplayName(),
                "CARD_KEY_VERSION=" + tag.getKeyVersion(),
                "CARD_ACTIVE=" + tag.isActive(),
                "CARD_LAST_READ_COUNTER=" + tag.getLastReadCounter(),
                "CARD_LAST_SEEN_AT=" + tag.getLastSeenAt());
    }

    /** Lädt den Nutzer. */
    private UserEntity user(String email) {
        return users.findByEmailIgnoreCase(email(email))
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
    }

    /** Lädt den aktiven Nutzer. */
    private UserEntity activeUser(String email, UserRole role) {
        UserEntity user = user(email);
        requireRole(user, role);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account must be an active " + role.name().toLowerCase(Locale.ROOT));
        }
        return user;
    }

    /** Prüft die erforderliche Rolle. */
    private void requireRole(UserEntity user, UserRole role) {
        if (user.getRole() != role) {
            throw new IllegalArgumentException("Account is not a " + role.name().toLowerCase(Locale.ROOT));
        }
    }

    /** Lädt den Studierenden. */
    private StudentEntity student(String email) {
        return students.findByUserEmailIgnoreCase(email(email))
                .orElseThrow(() -> new IllegalArgumentException("Student account not found"));
    }

    /** Lädt den Kurs. */
    private CourseGroupEntity course(String name) {
        CourseGroupEntity course = existingCourse(name);
        if (!course.isActive()) {
            throw new IllegalArgumentException("Course is inactive");
        }
        return course;
    }

    /** Lädt einen vorhandenen Kurs. */
    private CourseGroupEntity existingCourse(String name) {
        return courses.findByNameIgnoreCase(required(name, "course name", 100))
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }

    /** Prüft, ob die E-Mail-Adresse frei ist. */
    private void ensureEmailIsFree(String email) {
        ensureEmailIsFree(email, null);
    }

    /** Prüft, ob die E-Mail-Adresse frei ist. */
    private void ensureEmailIsFree(String email, UUID currentUserId) {
        users.findByEmailIgnoreCase(email)
                .filter(existing -> !Objects.equals(existing.getId(), currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email address already exists");
                });
    }

    /** Erstellt die Ausgabe zur Aktivierung. */
    private List<String> activationOutput(CreatedActivationCodeResponse result) {
        return List.of(
                "ADMIN_ACTIVATION_CODE=" + result.activationCode(),
                "ADMIN_ACTIVATION_CODE_EXPIRES_AT=" + result.expiresAt());
    }

    /** Erstellt die Ausgabe. */
    private List<String> output(List<String> lines) {
        return lines.isEmpty() ? List.of("NO_RESULTS") : lines;
    }

    /** Prüft und vereinheitlicht die E-Mail-Adresse. */
    private String email(String value) {
        String email = required(value, "email", 255).toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        return email;
    }

    /** Liest einen erforderlichen Wert. */
    private String required(String value, String name, int maxLength) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty() || result.length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1 to " + maxLength + " characters");
        }
        return result;
    }

    /** Gibt den Namen zurück. */
    private String name(UserEntity user) {
        return (text(user.getFirstName()) + " " + user.getLastName()).trim();
    }

    /** Ersetzt fehlenden Text durch einen leeren Wert. */
    private String text(String value) {
        return value == null ? "" : value;
    }

    /** Prüft die Anzahl der Argumente. */
    private void requireSize(List<String> args, int size, String usage) {
        if (args.size() != size) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    /** Erstellt den Fehler zur Befehlsverwendung. */
    private IllegalArgumentException usage() {
        return new IllegalArgumentException("Usage: ba-admin <secretary|lecturer|student|course|lecture|device|card> <command> [arguments]");
    }
}
