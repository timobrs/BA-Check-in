package de.jscoding.bacheckin.shared.dataSource

import de.jscoding.bacheckin.shared.model.AttendanceStatusDto
import de.jscoding.bacheckin.shared.model.BackendDeviceErrorCode
import de.jscoding.bacheckin.shared.model.BackendDtos
import de.jscoding.bacheckin.shared.model.BackendException
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import de.jscoding.bacheckin.shared.model.BackendRequests
import de.jscoding.bacheckin.shared.model.BackendResponses
import de.jscoding.bacheckin.shared.model.UserRole
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DemoBackendRemoteDataSource : BackendRemoteDataSource {

    private val absences = mutableListOf<BackendDtos.Absence>()
    private var absenceIdSequence = 0
    private var activeAppointmentId: String? = null
    private var protokollantIndex: Int = 0
    private val attendanceOverrides = mutableMapOf<String, MutableMap<String, AttendanceStatusDto>>()
    private val protokollantOverrides = mutableMapOf<String, String>()
    private val validatedAppointmentIds = mutableSetOf<String>()
    private val generatedActivationCodes = mutableMapOf<String, String>()
    private val revokedDeviceUserIds = mutableSetOf<String>()
    private val ignoredAppointments = mutableSetOf(
        DEMO_APPOINTMENT_IGNORED_ID,
        DEMO_APPOINTMENT_PAST_IGNORED_ID
    )
    private val ignoredModules = mutableSetOf(DEMO_MODULE_SECURITY_ID)

    private val consumedNfcPayloads = mutableSetOf<String>()
    private val lecturers = listOf(
        BackendDtos.Lecturer("demo-lecturer-1", "Alex Weber"),
        BackendDtos.Lecturer("demo-lecturer-2", "Samira Fischer")
    )
    private val moduleLecturers = mutableMapOf<String, String?>(DEMO_MODULE_DATABASES_ID to "demo-lecturer-1")
    private val appointmentLecturers = mutableMapOf<String, String?>()

    override suspend fun getStudentHomeData(): BackendResponses.StudentHomeData {
        delay(300.milliseconds)
        val appointments = demoVisibleAppointments()

        return BackendResponses.StudentHomeData(
            student = BackendDtos.Student(studentId = "123456"),
            absences = absences.toList(),
            checkInAvailability = BackendDtos.CheckInAvailability(
                state = "AVAILABLE",
                appointment = appointments.firstOrNull(),
                opensAt = appointments.firstOrNull()?.startAt?.minus(30.minutes),
                validUntil = appointments.firstOrNull()?.endAt
            ),
            appointments = appointments
        )
    }

    override suspend fun verifyNfcCheckIn(request: BackendRequests.NfcCheckInRequest): BackendResponses.NfcCheckInResult {
        delay(80.milliseconds)
        val payload = "${request.cardPublicId}:${request.encryptedPiccDataHex}:${request.sdmMacHex}"
        if (!consumedNfcPayloads.add(payload)) return BackendResponses.NfcCheckInResult(
            result = "ALREADY_USED", appointmentId = null, attendanceStatus = null,
            message = "Diese NFC-Nachricht wurde bereits verwendet."
        )
        val appointment = demoVisibleAppointments().firstOrNull()
        return BackendResponses.NfcCheckInResult(
            result = "CHECKED_IN", appointmentId = appointment?.id, attendanceStatus = "PRESENT",
            validUntilEpochMillis = appointment?.endAt?.toEpochMilliseconds()
        )
    }

    override suspend fun getDozentHomeData(): BackendResponses.DozentHomeData {
        delay(250.milliseconds)
        val appointments = demoLecturerAppointments()
        ensureActiveAppointment(appointments)
        return BackendResponses.DozentHomeData(
            appointmentOverview = dozentOverview(appointments)
        )
    }

    override suspend fun openAppointment(appointmentId: String): BackendResponses.AppointmentOverview {
        delay(120.milliseconds)
        val appointments = demoLecturerAppointments()
        require(appointments.any { it.id == appointmentId }) { "Termin ist diesem Dozenten nicht zugeordnet" }
        activeAppointmentId = appointments.firstOrNull { it.id == appointmentId }?.id ?: activeAppointmentId
        ensureActiveAppointment(appointments)
        return dozentOverview(appointments)
    }

    override suspend fun updateStudentAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay {
        delay(120.milliseconds)
        val appointments = demoLecturerAppointments()
        require(appointments.any { it.id == appointmentId }) { "Termin ist diesem Dozenten nicht zugeordnet" }
        if (appointments.any { it.id == appointmentId } && DEMO_STUDENTS.any { it.studentId == studentId }) {
            activeAppointmentId = appointmentId
            attendanceOverrides
                .getOrPut(appointmentId) { mutableMapOf() }[studentId] = status
            validatedAppointmentIds -= appointmentId
        }
        ensureActiveAppointment(appointments)
        return appointments.firstOrNull { it.id == appointmentId }
            ?.let { attendanceDay(it) }
            ?: attendanceDay(appointments.first())
    }

    override suspend fun validateAppointmentAttendances(
        appointmentId: String
    ): BackendResponses.AttendanceDay {
        delay(160.milliseconds)
        val appointments = demoLecturerAppointments()
        require(appointments.any { it.id == appointmentId }) { "Termin ist diesem Dozenten nicht zugeordnet" }
        if (appointments.any { it.id == appointmentId }) {
            activeAppointmentId = appointmentId
            validatedAppointmentIds += appointmentId
        }
        ensureActiveAppointment(appointments)
        return appointments.firstOrNull { it.id == appointmentId }
            ?.let { attendanceDay(it) }
            ?: attendanceDay(appointments.first())
    }

    override suspend fun selectAppointmentProtokollant(
        appointmentId: String,
        studentId: String
    ): BackendResponses.AppointmentOverview {
        delay(120.milliseconds)
        val appointments = demoLecturerAppointments()
        require(appointments.any { it.id == appointmentId }) { "Termin ist diesem Dozenten nicht zugeordnet" }
        val selectedStudent = DEMO_STUDENTS.firstOrNull { it.studentId == studentId }
        if (appointments.any { it.id == appointmentId } && selectedStudent != null) {
            activeAppointmentId = appointmentId
            protokollantOverrides[appointmentId] = selectedStudent.displayName ?: selectedStudent.studentId
        }
        val updatedAppointments = demoLecturerAppointments()
        ensureActiveAppointment(updatedAppointments)
        return dozentOverview(updatedAppointments)
    }

    override suspend fun getVerwaltungHomeData(): BackendResponses.VerwaltungHomeData {
        delay(180.milliseconds)
        return BackendResponses.VerwaltungHomeData(
            courseDetails = demoCourses().map { course ->
                val appointments = demoAppointmentsForCourse(course.id)
                BackendResponses.CourseDetail(
                    course = course,
                    modules = demoModules().filter { it.courseGroupId == course.id },
                    students = demoStudents(),
                    appointments = appointments,
                    attendanceDays = appointments
                        .filter { it.id !in ignoredAppointments && it.moduleId !in ignoredModules }
                        .map(::attendanceDay)
                )
            },
            lecturers = lecturers
        )
    }

    override suspend fun createVerwaltungUserActivationCode(studentId: String): BackendDtos.Student {
        delay(160.milliseconds)
        val nextCode = "STUDENT${Clock.System.now().toEpochMilliseconds().toString().takeLast(8)}"
        generatedActivationCodes[studentId] = nextCode
        return demoStudents().first { it.studentId == studentId }
    }

    override suspend fun revokeVerwaltungUserDevice(studentId: String): BackendDtos.Student {
        delay(160.milliseconds)
        revokedDeviceUserIds += studentId
        return demoStudents().first { it.studentId == studentId }
    }

    override suspend fun updateVerwaltungAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay {
        delay(120.milliseconds)
        attendanceOverrides
            .getOrPut(appointmentId) { mutableMapOf() }[studentId] = status
        validatedAppointmentIds -= appointmentId
        return demoAppointments()
            .first { it.id == appointmentId }
            .let(::attendanceDay)
    }

    override suspend fun validateVerwaltungAttendances(appointmentId: String): BackendResponses.AttendanceDay {
        delay(160.milliseconds)
        validatedAppointmentIds += appointmentId
        return demoAppointments()
            .first { it.id == appointmentId }
            .let(::attendanceDay)
    }

    override suspend fun validateActivationCode(activationCode: String) {
        delay(120.milliseconds)

        val normalized = activationCode.trim()
        if (normalized.length < 8) {
            throw BackendException(
                code = BackendDeviceErrorCode.INVALID_ACTIVATION_CODE,
                message = "Der Freischaltungscode ist zu kurz."
            )
        }

        when (normalized.uppercase()) {
            "EXPIRED", "DEMO-EXPIRED" -> throw BackendException(
                code = BackendDeviceErrorCode.ACTIVATION_CODE_EXPIRED,
                message = "Der Freischaltungscode ist abgelaufen."
            )

            "USED", "DEMO-USED" -> throw BackendException(
                code = BackendDeviceErrorCode.ACTIVATION_CODE_ALREADY_USED,
                message = "Der Freischaltungscode wurde bereits verwendet."
            )
        }
    }

    override suspend fun createActivationChallenge(): BackendResponses.ActivationChallenge {
        delay(120.milliseconds)
        return BackendResponses.ActivationChallenge(
            activationSessionId = "demo-activation-session",
            challengeBase64 = "ZGVtby1hY3RpdmF0aW9uLWNoYWxsZW5nZQ=="
        )
    }

    override suspend fun completeActivation(
        request: BackendRequests.CompleteActivation
    ): BackendResponses.CompleteActivation {
        delay(200.milliseconds)

        validateActivationCode(request.activationCode)

        val deviceSuffix = request.activationCode
            .trim()
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .ifBlank { "DEMO" }
            .takeLast(8)

        val userRole = when {
            request.activationCode.contains("STUDENT", ignoreCase = true) -> UserRole.STUDENT
            request.activationCode.contains("DOZENT", ignoreCase = true) -> UserRole.DOZENT
            request.activationCode.contains("VERWALTUNG", ignoreCase = true) -> UserRole.VERWALTUNG
            else -> UserRole.VERWALTUNG
        }

        return BackendResponses.CompleteActivation(
            userId = "demo-user-1001",
            userRole = userRole,
            displayName = "Demo User",
            deviceId = "demo-device-$deviceSuffix",
            deviceCredentialId = "demo-device-$deviceSuffix"
        )
    }

    override suspend fun createAuthChallenge(
        request: BackendRequests.AuthChallenge
    ): BackendResponses.AuthChallenge {
        delay(120.milliseconds)

        if (request.deviceCredentialId.isBlank()) {
            throw BackendException(
                code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
                message = "Gerät ist nicht aktiv."
            )
        }

        return BackendResponses.AuthChallenge(
            authSessionId = "auth-${request.deviceCredentialId.takeLast(8)}",
            challengeBase64 = "ZGVtby1hdXRoLWNoYWxsZW5nZQ=="
        )
    }

    override suspend fun openDeviceSession(
        request: BackendRequests.DeviceSession
    ): BackendResponses.DeviceSession {
        delay(150.milliseconds)

        if (request.deviceCredentialId.isBlank()) {
            throw BackendException(
                code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
                message = "Gerät ist nicht aktiv."
            )
        }

        if (request.signatureBase64.isBlank()) {
            throw BackendException(
                code = BackendDeviceErrorCode.UNKNOWN,
                message = "Ungültige Signatur."
            )
        }

        val now = Clock.System.now().toEpochMilliseconds()

        return BackendResponses.DeviceSession(
            accessToken = demoAccessToken(
                prefix = "session",
                value = request.deviceCredentialId + ":" + request.authSessionId
            ),
            refreshToken = demoAccessToken(
                prefix = "refresh",
                value = request.deviceCredentialId
            ),
            accessTokenExpiresAtEpochMillis = now + ACCESS_TOKEN_LIFETIME_MS
        )
    }

    override suspend fun refresh(request: BackendRequests.Refresh): BackendResponses.Refresh {
        delay(150.milliseconds)

        if (request.deviceCredentialId.isBlank()) {
            throw BackendException(
                code = BackendDeviceErrorCode.DEVICE_NOT_ACTIVE,
                message = "Gerät ist nicht aktiv."
            )
        }

        if (request.refreshToken.isBlank()) {
            throw BackendException(
                code = BackendDeviceErrorCode.UNKNOWN,
                message = "Ungültiges Refresh-Token."
            )
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val newRefresh = demoAccessToken(
            prefix = "refresh",
            value = request.deviceCredentialId + ":" + now
        )

        return BackendResponses.Refresh(
            accessToken = demoAccessToken(
                prefix = "refresh_access",
                value = request.deviceCredentialId + ":" + now
            ),
            refreshToken = newRefresh,
            accessTokenExpiresAtEpochMillis = now + ACCESS_TOKEN_LIFETIME_MS
        )
    }

    override suspend fun logoutDevice(deviceCredentialId: String) {
        delay(60.milliseconds)
        if (deviceCredentialId.isBlank()) {
            return
        }
    }

    override suspend fun unlinkCurrentDevice() {
        delay(60.milliseconds)
    }

    override suspend fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long) {
        delay(1000.milliseconds)
        absenceIdSequence += 1
        absences += BackendDtos.Absence(
            fromEpochMillis = fromEpochMillis,
            untilEpochMillis = untilEpochMillis,
            id = "absence-$absenceIdSequence"
        )
    }

    override suspend fun importTrainexCalendar(
        sourceKey: String,
        icsContent: String
    ): BackendResponses.CalendarImportResult = BackendResponses.CalendarImportResult(
        created = 0,
        updated = 0,
        archived = 0,
        skipped = 0
    )

    override suspend fun assignStudentModule(studentId: String, moduleId: String) = Unit
    override suspend fun removeStudentModule(studentId: String, moduleId: String) = Unit
    override suspend fun updateLectureModule(appointmentId: String, moduleId: String) = Unit
    override suspend fun updateVerwaltungAppointmentIgnored(appointmentId: String, ignored: Boolean) {
        if (ignored) ignoredAppointments += appointmentId else ignoredAppointments -= appointmentId
    }

    override suspend fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean) {
        if (ignored) ignoredModules += moduleId else ignoredModules -= moduleId
    }

    override suspend fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?) {
        require(lecturerId == null || lecturers.any { it.id == lecturerId && it.active })
        moduleLecturers[moduleId] = lecturerId
    }

    override suspend fun updateVerwaltungAppointmentLecturer(appointmentId: String, overrideModule: Boolean, lecturerId: String?) {
        require(demoAppointments().any { it.id == appointmentId && it.lecturerEditable })
        require(lecturerId == null || lecturers.any { it.id == lecturerId && it.active })
        if (overrideModule) appointmentLecturers[appointmentId] = lecturerId else appointmentLecturers.remove(appointmentId)
    }
    override suspend fun reviewAbsence(absenceId: String, status: String) = Unit

    private fun dozentOverview(
        appointments: List<BackendDtos.Appointment>
    ): BackendResponses.AppointmentOverview {
        val visibleActiveAppointmentId = visibleActiveAppointmentId(appointments)
        return BackendResponses.AppointmentOverview(
            appointments = appointments,
            activeAppointmentId = visibleActiveAppointmentId,
            activeAttendanceDay = visibleActiveAppointmentId
                ?.let { appointmentId -> appointments.firstOrNull { it.id == appointmentId } }
                ?.let { attendanceDay(it) }
        )
    }

    private fun ensureActiveAppointment(appointments: List<BackendDtos.Appointment>) {
        if (visibleActiveAppointmentId(appointments) != null) {
            return
        }
    }

    private fun visibleActiveAppointmentId(appointments: List<BackendDtos.Appointment>): String? {
        val now = Clock.System.now()
        val selectedVisibleId = activeAppointmentId
            ?.takeIf { selectedId ->
                appointments.firstOrNull { it.id == selectedId }
                    ?.isVisibleAsActive(now)
                    ?: false
            }

        activeAppointmentId = selectedVisibleId ?: appointments.firstOrNull {
            it.isVisibleAsActive(now)
        }?.id

        return activeAppointmentId
    }

    private fun BackendDtos.Appointment.isVisibleAsActive(now: Instant): Boolean {
        val isRunning = now in startAt..endAt
        val isEndedAndUnconfirmed = endAt < now && id !in validatedAppointmentIds
        return isRunning || isEndedAndUnconfirmed
    }

    private fun demoAppointments(): List<BackendDtos.Appointment> {
        val now = Clock.System.now()
        val protokollant = DEMO_PROTOKOLLANTEN[protokollantIndex]

        return listOf(
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_INTRO_ID,
                title = "Einführung in die Informatik",
                room = "203",
                course = "WS-26-IV",
                protokollant = protokollantOverrides[DEMO_APPOINTMENT_INTRO_ID] ?: protokollant,
                startAt = now - 30.minutes,
                endAt = now + 90.minutes,
                moduleId = DEMO_MODULE_INTRO_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_DATABASES_ID,
                title = "Datenbanken & Architekturen",
                room = "102",
                course = "WS-24-II",
                protokollant = protokollantOverrides[DEMO_APPOINTMENT_DATABASES_ID]
                    ?: "Maja Müller",
                startAt = now + 3.hours,
                endAt = now + 6.hours,
                moduleId = DEMO_MODULE_DATABASES_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_PROJECT_ID,
                title = "Projektmanagement",
                room = "103",
                course = "WS-22-III",
                protokollant = protokollantOverrides[DEMO_APPOINTMENT_PROJECT_ID]
                    ?: "Noah Schneider",
                startAt = now + 24.hours,
                endAt = now + 26.hours,
                moduleId = DEMO_MODULE_PROJECT_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_SOFTWARE_ID,
                title = "Software Engineering",
                room = "204",
                course = "WS-26-IV",
                protokollant = "Sofia Keller",
                startAt = now + 2.hours,
                endAt = now + 4.hours,
                moduleId = DEMO_MODULE_SOFTWARE_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_IGNORED_ID,
                title = "Organisatorischer Termin",
                room = "Aula",
                course = "WS-26-IV",
                protokollant = "Lea Hofmann",
                startAt = now + 8.hours,
                endAt = now + 10.hours,
                moduleId = DEMO_MODULE_ORGANISATION_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_SECURITY_ID,
                title = "IT-Sicherheit",
                room = "105",
                course = "WS-24-II",
                protokollant = "Max Mustermann",
                startAt = now + 12.hours,
                endAt = now + 14.hours,
                moduleId = DEMO_MODULE_SECURITY_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_PAST_IGNORED_ID,
                title = "Vergangener ignorierter Termin",
                room = "101",
                course = "WS-24-II",
                protokollant = "Maja Müller",
                startAt = now - 4.hours,
                endAt = now - 2.hours,
                moduleId = DEMO_MODULE_DATABASES_ID
            ),
            BackendDtos.Appointment(
                id = DEMO_APPOINTMENT_WEB_ID,
                title = "Web-Design und Standards",
                room = "106",
                course = "WS-22-III",
                protokollant = "Noah Schneider",
                startAt = now + 48.hours,
                endAt = now + 50.hours,
                moduleId = DEMO_MODULE_WEB_ID
            )
        ).map { appointment ->
            val editable = appointment.startAt > now && appointment.id != activeAppointmentId
            appointment.copy(
                ignored = appointment.id in ignoredAppointments,
                lecturerId = if (editable) {
                    if (appointmentLecturers.containsKey(appointment.id)) appointmentLecturers[appointment.id]
                    else moduleLecturers[appointment.moduleId]
                } else "demo-lecturer-1",
                lecturerOverride = appointmentLecturers.containsKey(appointment.id),
                lecturerEditable = editable
            )
        }
    }

    private fun demoVisibleAppointments(): List<BackendDtos.Appointment> =
        demoAppointments().filter { appointment ->
            !appointment.ignored && appointment.moduleId !in ignoredModules
        }

    private fun demoLecturerAppointments(): List<BackendDtos.Appointment> =
        demoVisibleAppointments().filter { it.lecturerId == "demo-lecturer-1" }

    private fun demoAttendances(appointmentId: String): List<BackendDtos.Attendance> {
        val overrides = attendanceOverrides[appointmentId].orEmpty()
        return DEMO_STUDENTS.map { student ->
            val status = overrides[student.studentId] ?: DEMO_ATTENDANCE_STATUSES[student.studentId]
            BackendDtos.Attendance(
                student = student,
                status = status ?: AttendanceStatusDto.MISSING,
                lateMinutes = if (status == AttendanceStatusDto.LATE) 10 else null
            )
        }
    }

    private fun demoCourses(): List<BackendDtos.Course> =
        demoAppointments()
            .map { appointment ->
                BackendDtos.Course(
                    id = appointment.course,
                    name = appointment.course
                )
            }
            .distinctBy { it.id }

    private fun demoModules(): List<BackendDtos.Module> = listOf(
        BackendDtos.Module(DEMO_MODULE_INTRO_ID, "WS-26-IV", "Einführung in die Informatik"),
        BackendDtos.Module(DEMO_MODULE_SOFTWARE_ID, "WS-26-IV", "Software Engineering"),
        BackendDtos.Module(DEMO_MODULE_ORGANISATION_ID, "WS-26-IV", "Organisation (ignoriert testweise)"),
        BackendDtos.Module(DEMO_MODULE_DATABASES_ID, "WS-24-II", "Datenbanken & Architekturen"),
        BackendDtos.Module(DEMO_MODULE_SECURITY_ID, "WS-24-II", "IT-Sicherheit"),
        BackendDtos.Module(DEMO_MODULE_PROJECT_ID, "WS-22-III", "Projektmanagement"),
        BackendDtos.Module(DEMO_MODULE_WEB_ID, "WS-22-III", "Web-Design und Standards")
    ).map { module ->
        module.copy(ignored = module.id in ignoredModules, lecturerId = moduleLecturers[module.id])
    }

    private fun demoAppointmentsForCourse(courseId: String): List<BackendDtos.Appointment> {
        val appointments = demoAppointments()
        return appointments.filter { it.course == courseId }.ifEmpty { appointments }
    }

    private fun demoStudents(): List<BackendDtos.Student> =
        DEMO_STUDENTS.map { student ->
            student.copy(
                activatedSinceEpochMillis = demoActivatedSinceEpochMillis(student.studentId),
                generatedActivationCode = generatedActivationCodes[student.studentId]
            )
        }

    private fun demoActivatedSinceEpochMillis(studentId: String): Long? =
        if (studentId in revokedDeviceUserIds) {
            null
        } else {
            Clock.System.now()
                .minus(24.hours)
                .minus(studentId.takeLast(2).toInt().minutes)
                .toEpochMilliseconds()
        }

    private fun attendanceDay(appointment: BackendDtos.Appointment): BackendResponses.AttendanceDay =
        BackendResponses.AttendanceDay(
            appointment = appointment,
            attendances = demoAttendances(appointment.id),
            attendancesValidated = appointment.id in validatedAppointmentIds
        )

    private fun demoAccessToken(prefix: String, value: String): String =
        "$prefix-${value.hashCode().toString(16)}"

    private companion object {
        private const val ACCESS_TOKEN_LIFETIME_MS = 120_000L
        private const val DEMO_APPOINTMENT_INTRO_ID = "11111111-1111-1111-1111-111111111111"
        private const val DEMO_APPOINTMENT_DATABASES_ID = "22222222-2222-2222-2222-222222222222"
        private const val DEMO_APPOINTMENT_PROJECT_ID = "33333333-3333-3333-3333-333333333333"
        private const val DEMO_APPOINTMENT_SOFTWARE_ID = "44444444-4444-4444-4444-444444444444"
        private const val DEMO_APPOINTMENT_IGNORED_ID = "55555555-5555-5555-5555-555555555555"
        private const val DEMO_APPOINTMENT_SECURITY_ID = "66666666-6666-6666-6666-666666666666"
        private const val DEMO_APPOINTMENT_PAST_IGNORED_ID = "77777777-7777-7777-7777-777777777777"
        private const val DEMO_APPOINTMENT_WEB_ID = "88888888-8888-8888-8888-888888888888"

        private const val DEMO_MODULE_INTRO_ID = "demo-module-intro"
        private const val DEMO_MODULE_SOFTWARE_ID = "demo-module-software"
        private const val DEMO_MODULE_ORGANISATION_ID = "demo-module-organisation"
        private const val DEMO_MODULE_DATABASES_ID = "demo-module-databases"
        private const val DEMO_MODULE_SECURITY_ID = "demo-module-security"
        private const val DEMO_MODULE_PROJECT_ID = "demo-module-project"
        private const val DEMO_MODULE_WEB_ID = "demo-module-web"

        private val DEMO_PROTOKOLLANTEN = listOf(
            "Max Mustermann",
            "Sofia Keller",
            "Lea Hofmann"
        )

        private val DEMO_STUDENTS = listOf(
            BackendDtos.Student("123456", "Max Mustermann"),
            BackendDtos.Student("123457", "Aylin Demir"),
            BackendDtos.Student("123458", "Jonas Weber"),
            BackendDtos.Student("123459", "Maja Müller"),
            BackendDtos.Student("123460", "Noah Schneider"),
            BackendDtos.Student("123461", "Sofia Keller"),
            BackendDtos.Student("123462", "Lea Hofmann"),
            BackendDtos.Student("123463", "Elias Braun"),
            BackendDtos.Student("123464", "Mina Becker"),
            BackendDtos.Student("123465", "Tim Fischer"),
            BackendDtos.Student("123466", "Hanna Wagner"),
            BackendDtos.Student("123467", "Luca Neumann")
        )

        private val DEMO_ATTENDANCE_STATUSES = mapOf(
            "123456" to AttendanceStatusDto.MISSING,
            "123457" to AttendanceStatusDto.MISSING,
            "123458" to AttendanceStatusDto.MISSING,
            "123459" to AttendanceStatusDto.PRESENT,
            "123460" to AttendanceStatusDto.LATE,
            "123461" to AttendanceStatusDto.PRESENT,
            "123462" to AttendanceStatusDto.PRESENT,
            "123463" to AttendanceStatusDto.PRESENT,
            "123464" to AttendanceStatusDto.PRESENT,
            "123465" to AttendanceStatusDto.ABSENT,
            "123466" to AttendanceStatusDto.ABSENT,
            "123467" to AttendanceStatusDto.ABSENT
        )
    }
}
