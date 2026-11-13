package de.jscoding.bacheckin.shared.dataSource

import de.jscoding.bacheckin.shared.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class PostgresBackendRemoteDataSource(
    private val config: BackendRuntimeConfig,
    private val httpClient: HttpClient = defaultHttpClient()
) : BackendRemoteDataSource, BackendAccessTokenReceiver {
    constructor(config: BackendRuntimeConfig) : this(config, defaultHttpClient())

    private var currentAccessToken: String? = null
    private var preparedActivationChallenge: BackendResponses.ActivationChallenge? = null

    override fun setAccessToken(accessToken: String?) {
        currentAccessToken = accessToken
    }

    override suspend fun getStudentHomeData() =
        get<StudentHomeWire>("/api/v1/student/home", true).toContract()

    override suspend fun getDozentHomeData() =
        get<DozentHomeWire>("/api/v1/lecturer/home", true).toContract()

    override suspend fun openAppointment(appointmentId: String) =
        post<AppointmentOverviewWire>(
            "/api/v1/lecturer/appointments/$appointmentId/open",
            authenticated = true
        ).toContract()

    override suspend fun updateStudentAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ) = put<AttendanceDayWire>(
        "/api/v1/lecturer/appointments/$appointmentId/attendances/$studentId",
        AttendanceUpdateWire(status.name),
        true
    ).toContract()

    override suspend fun validateAppointmentAttendances(appointmentId: String) =
        post<AttendanceDayWire>(
            "/api/v1/lecturer/appointments/$appointmentId/attendances/validate",
            authenticated = true
        ).toContract()

    override suspend fun selectAppointmentProtokollant(appointmentId: String, studentId: String) =
        put<AppointmentOverviewWire>(
            "/api/v1/lecturer/appointments/$appointmentId/protokollant",
            ProtokollantWire(studentId),
            true
        ).toContract()

    override suspend fun getVerwaltungHomeData() =
        get<SecretaryHomeWire>("/api/v1/secretary/home", true).toContract()

    override suspend fun createVerwaltungUserActivationCode(studentId: String): BackendDtos.Student {
        val activation = post<ActivationCodeWire>(
            "/api/v1/secretary/users/$studentId/device-activation-codes",
            AutomaticActivationWire(),
            true
        )
        return BackendDtos.Student(
            studentId = studentId,
            generatedActivationCode = activation.activationCode
        )
    }

    override suspend fun revokeVerwaltungUserDevice(studentId: String): BackendDtos.Student {
        post<Unit>(
            "/api/v1/secretary/users/$studentId/device/revoke",
            RevokeWire("Revoked by secretary"),
            true
        )
        return BackendDtos.Student(studentId = studentId)
    }

    override suspend fun updateVerwaltungAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ) = put<AttendanceDayWire>(
        "/api/v1/secretary/appointments/$appointmentId/attendances/$studentId",
        AttendanceUpdateWire(status.name),
        true
    ).toContract()

    override suspend fun validateVerwaltungAttendances(appointmentId: String) =
        post<AttendanceDayWire>(
            "/api/v1/secretary/appointments/$appointmentId/attendances/validate",
            authenticated = true
        ).toContract()

    override suspend fun validateActivationCode(activationCode: String) {
        preparedActivationChallenge = null
        preparedActivationChallenge = post<ActivationChallengeWire>(
            "/api/v1/device-activations/challenges",
            ActivationChallengeRequestWire(activationCode.trim())
        ).toContract()
    }

    override suspend fun createActivationChallenge() = preparedActivationChallenge
        ?: throw BackendException(
            BackendDeviceErrorCode.INVALID_ACTIVATION_CODE,
            "Activation code must be validated first"
        )

    override suspend fun completeActivation(request: BackendRequests.CompleteActivation): BackendResponses.CompleteActivation {
        val device = request.deviceInfo
        val isIos = device.manufacturer.equals("Apple", ignoreCase = true)
        val activation = CompleteActivationWire(
            activationSessionId = request.activationSessionId,
            platform = if (isIos) "IOS" else "ANDROID",
            deviceName = "${device.manufacturer} ${device.model}".trim(),
            manufacturer = device.manufacturer,
            model = device.model,
            osVersion = if (isIos) "iOS" else "Android ${device.androidSdk}",
            appVersion = "${device.appVersionName} (${device.appVersionCode})",
            publicKeyBase64 = request.publicKeyBase64,
            challengeSignatureBase64 = request.challengeSignatureBase64
        )
        val response = post<ActivationResponseWire>(
            "/api/v1/device-activations",
            activation
        )
        preparedActivationChallenge = null
        return response.toContract()
    }

    override suspend fun createAuthChallenge(request: BackendRequests.AuthChallenge) =
        post<AuthChallengeWire>(
            "/api/v1/device-sessions/challenges",
            DeviceCredentialWire(request.deviceCredentialId)
        ).toContract()

    override suspend fun openDeviceSession(request: BackendRequests.DeviceSession) =
        post<TokenPairWire>(
            "/api/v1/device-sessions/open",
            DeviceSessionWire(
                request.deviceCredentialId,
                request.authSessionId,
                request.timestampEpochMillis,
                request.signatureBase64
            )
        ).toDeviceSession()

    override suspend fun refresh(request: BackendRequests.Refresh) =
        post<TokenPairWire>(
            "/api/v1/device-sessions/refresh",
            RefreshWire(
                request.deviceCredentialId,
                request.refreshToken,
                request.timestampEpochMillis,
                request.signatureBase64
            )
        ).toRefresh()

    override suspend fun logoutDevice(deviceCredentialId: String) {
        post<Unit>("/api/v1/device-sessions/logout", authenticated = true)
    }

    override suspend fun unlinkCurrentDevice() {
        execute<Unit>(HttpMethod.Delete, "/api/v1/devices/current", authenticated = true)
    }
    override suspend fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long) {
        post<Unit>(
            "/api/v1/student/absences",
            AbsenceRequestWire(fromEpochMillis, untilEpochMillis),
            true
        )
    }

    override suspend fun importTrainexCalendar(sourceKey: String, icsContent: String) =
        execute<CalendarImportWire>(
            HttpMethod.Put,
            "/api/v1/secretary/calendar-imports/$sourceKey",
            true
        ) {
            contentType(ContentType.parse("text/calendar"))
            setBody(icsContent)
        }.let { result ->
            BackendResponses.CalendarImportResult(
                result.created,
                result.updated,
                result.archived,
                result.skipped
            )
        }

    override suspend fun verifyNfcCheckIn(request: BackendRequests.NfcCheckInRequest) =
        post<NfcCheckInResultWire>(
            "/api/v1/student/nfc/check-ins",
            NfcCheckInRequestWire(
                request.cardPublicId,
                request.encryptedPiccDataHex,
                request.sdmMacHex
            ),
            true
        ).toContract()

    override suspend fun assignStudentModule(studentId: String, moduleId: String) {
        execute<Unit>(HttpMethod.Put, "/api/v1/secretary/users/$studentId/modules/$moduleId", true)
    }

    override suspend fun removeStudentModule(studentId: String, moduleId: String) {
        execute<Unit>(HttpMethod.Delete, "/api/v1/secretary/users/$studentId/modules/$moduleId", true)
    }

    override suspend fun updateLectureModule(appointmentId: String, moduleId: String) {
        put<Unit>(
            "/api/v1/secretary/appointments/$appointmentId/module",
            ModuleAssignmentWire(moduleId),
            true
        )
    }

    override suspend fun updateVerwaltungAppointmentIgnored(appointmentId: String, ignored: Boolean) {
        put<Unit>(
            "/api/v1/secretary/appointments/$appointmentId/ignored",
            IgnoreUpdateWire(ignored),
            true
        )
    }

    override suspend fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?) {
        put<Unit>("/api/v1/secretary/modules/$moduleId/lecturer", LecturerUpdateWire(lecturerId), true)
    }

    override suspend fun updateVerwaltungAppointmentLecturer(appointmentId: String, overrideModule: Boolean, lecturerId: String?) {
        put<Unit>("/api/v1/secretary/appointments/$appointmentId/lecturer",
            LectureLecturerUpdateWire(overrideModule, lecturerId), true)
    }

    override suspend fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean) {
        put<Unit>(
            "/api/v1/secretary/modules/$moduleId/ignored",
            IgnoreUpdateWire(ignored),
            true
        )
    }

    override suspend fun reviewAbsence(absenceId: String, status: String) {
        put<Unit>("/api/v1/secretary/absences/$absenceId", AbsenceReviewWire(status), true)
    }

    private suspend inline fun <reified T> get(
        path: String,
        authenticated: Boolean = false
    ): T = execute(HttpMethod.Get, path, authenticated)

    private suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        authenticated: Boolean = false
    ): T = execute(HttpMethod.Post, path, authenticated) {
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend inline fun <reified T> put(
        path: String,
        body: Any,
        authenticated: Boolean
    ): T = execute(HttpMethod.Put, path, authenticated) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend inline fun <reified T> execute(
        method: HttpMethod,
        path: String,
        authenticated: Boolean,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val response = try {
            httpClient.request("${config.normalizedBaseUrl}$path") {
                this.method = method
                accept(ContentType.Application.Json)
                if (authenticated) {
                    bearerAuth(requireAccessToken())
                }
                block()
            }
        } catch (error: BackendException) {
            throw error
        } catch (error: Throwable) {
            throw BackendException(
                BackendDeviceErrorCode.NETWORK_ERROR,
                error.message,
                error
            )
        }
        if (!response.status.isSuccess()) {
            throw response.toBackendException()
        }
        if (T::class == Unit::class) {
            @Suppress("UNCHECKED_CAST")
            return Unit as T
        }
        return response.body()
    }

    private fun requireAccessToken() = currentAccessToken?.takeIf(String::isNotBlank)
        ?: throw BackendException(BackendDeviceErrorCode.DEVICE_NOT_ACTIVE, "Missing access token")

    private suspend fun HttpResponse.toBackendException(): BackendException {
        val errorDetails = runCatching { body<ProblemWire>() }.getOrNull()
        val errorCode = when (errorDetails?.code) {
            "ACTIVATION_CODE_NOT_USABLE" -> BackendDeviceErrorCode.INVALID_ACTIVATION_CODE
            "DEVICE_KEY_REJECTED" -> BackendDeviceErrorCode.DEVICE_KEY_REJECTED
            "RATE_LIMITED" -> BackendDeviceErrorCode.RATE_LIMITED
            "DEVICE_NOT_FOUND" -> BackendDeviceErrorCode.DEVICE_NOT_ACTIVE
            "DEVICE_AUTHENTICATION_FAILED", "INVALID_REFRESH_TOKEN" -> BackendDeviceErrorCode.DEVICE_REVOKED
            else -> if (status == HttpStatusCode.TooManyRequests) {
                BackendDeviceErrorCode.RATE_LIMITED
            } else {
                BackendDeviceErrorCode.UNKNOWN
            }
        }
        return BackendException(
            errorCode,
            errorDetails?.detail ?: "Backend request failed (${status.value})"
        )
    }

    companion object {
        private fun defaultHttpClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
        }
    }
}

@Serializable
private data class ProblemWire(
    val code: String? = null,
    val detail: String? = null
)

@Serializable
private data class ActivationChallengeRequestWire(val activationCode: String)

@Serializable
private data class ActivationChallengeWire(
    val activationSessionId: String,
    val challengeBase64Url: String
)

@Serializable
private data class CompleteActivationWire(
    val activationSessionId: String,
    val platform: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val publicKeyBase64: String,
    val challengeSignatureBase64: String
)

@Serializable
private data class ActivationResponseWire(
    val userId: String,
    val userRole: String,
    val displayName: String? = null,
    val deviceId: String,
    val deviceCredentialId: String
)

@Serializable
private data class DeviceCredentialWire(val deviceCredentialId: String)

@Serializable
private data class AuthChallengeWire(
    val authSessionId: String,
    val challengeBase64Url: String
)

@Serializable
private data class DeviceSessionWire(
    val deviceCredentialId: String,
    val authSessionId: String,
    val timestampEpochMillis: Long,
    val signatureBase64: String
)

@Serializable
private data class RefreshWire(
    val deviceCredentialId: String,
    val refreshToken: String,
    val timestampEpochMillis: Long,
    val signatureBase64: String
)

@Serializable
private data class TokenPairWire(
    val accessToken: String,
    val refreshToken: String? = null,
    val accessTokenExpiresAt: String
)

@Serializable
private data class AttendanceUpdateWire(val status: String)

@Serializable
private data class ProtokollantWire(val studentId: String)

@Serializable
private class AutomaticActivationWire

@Serializable
private data class ActivationCodeWire(val activationCode: String)

@Serializable
private data class RevokeWire(val reason: String)

@Serializable
private data class AbsenceRequestWire(
    val fromEpochMillis: Long,
    val untilEpochMillis: Long
)

@Serializable
private data class CalendarImportWire(
    val created: Int,
    val updated: Int,
    val archived: Int,
    val skipped: Int
)

@Serializable
private data class NfcCheckInRequestWire(
    val cardPublicId: String,
    val encryptedPiccDataHex: String,
    val sdmMacHex: String
)
@Serializable
private data class NfcCheckInResultWire(
    val result: String,
    val appointmentId: String? = null,
    val attendanceStatus: String? = null,
    val validUntilEpochMillis: Long? = null,
    val message: String? = null
)

@Serializable
private data class ModuleAssignmentWire(val moduleId: String)

@Serializable
private data class AbsenceReviewWire(val status: String)

@Serializable
private data class StudentWire(
    val studentId: String,
    val displayName: String? = null,
    val activatedSinceEpochMillis: Long? = null,
    val generatedActivationCode: String? = null
)

@Serializable
private data class CourseWire(val id: String, val name: String)

@Serializable
private data class ModuleWire(
    val id: String,
    val courseGroupId: String,
    val name: String,
    val ignored: Boolean = false,
    val lecturerId: String? = null
)

@Serializable
private data class LecturerWire(val id: String, val displayName: String, val active: Boolean)

@Serializable
private data class LecturerUpdateWire(val lecturerId: String?)

@Serializable
private data class LectureLecturerUpdateWire(val overrideModule: Boolean, val lecturerId: String?)

@Serializable
private data class ModuleEnrollmentWire(
    val studentId: String,
    val moduleId: String
)

@Serializable
private data class AppointmentWire(
    val id: String,
    val title: String,
    val room: String,
    val course: String,
    val protokollant: String,
    val startAt: String,
    val endAt: String,
    val moduleId: String? = null,
    val ignored: Boolean = false,
    val lecturerId: String? = null,
    val lecturerOverride: Boolean = false,
    val lecturerEditable: Boolean = false
)

@Serializable
private data class IgnoreUpdateWire(val ignored: Boolean)

@Serializable
private data class AbsenceWire(
    val fromEpochMillis: Long,
    val untilEpochMillis: Long,
    val reason: String? = null,
    val id: String = "",
    val status: String = "SUBMITTED"
)

@Serializable
private data class CheckInAvailabilityWire(
    val state: String,
    val appointment: AppointmentWire? = null,
    val opensAt: String? = null,
    val validUntil: String? = null,
    val attendanceStatus: String? = null
)

@Serializable
private data class AbsenceReviewWireResponse(
    val id: String,
    val student: StudentWire,
    val fromEpochMillis: Long,
    val untilEpochMillis: Long,
    val reason: String? = null,
    val status: String
)

@Serializable
private data class AttendanceWire(
    val student: StudentWire,
    val status: String,
    val lateMinutes: Int? = null
)

@Serializable
private data class AttendanceDayWire(
    val appointment: AppointmentWire,
    val attendances: List<AttendanceWire>,
    val attendancesValidated: Boolean
)

@Serializable
private data class AppointmentOverviewWire(
    val appointments: List<AppointmentWire>,
    val activeAppointmentId: String? = null,
    val activeAttendanceDay: AttendanceDayWire? = null
)

@Serializable
private data class StudentHomeWire(
    val student: StudentWire,
    val absences: List<AbsenceWire>,
    val checkInAvailability: CheckInAvailabilityWire,
    val appointments: List<AppointmentWire>
)

@Serializable
private data class DozentHomeWire(val appointmentOverview: AppointmentOverviewWire)

@Serializable
private data class CourseDetailWire(
    val course: CourseWire,
    val modules: List<ModuleWire> = emptyList(),
    val students: List<StudentWire>,
    val appointments: List<AppointmentWire>,
    val attendanceDays: List<AttendanceDayWire>
)

@Serializable
private data class SecretaryHomeWire(
    val courseDetails: List<CourseDetailWire>,
    val moduleEnrollments: List<ModuleEnrollmentWire> = emptyList(),
    val absenceReviews: List<AbsenceReviewWireResponse> = emptyList(),
    val lecturers: List<LecturerWire> = emptyList()
)

private fun ActivationChallengeWire.toContract() = BackendResponses.ActivationChallenge(
    activationSessionId,
    challengeBase64Url
)

private fun ActivationResponseWire.toContract() = BackendResponses.CompleteActivation(
    userId,
    userRole.toAppRole(),
    displayName,
    deviceId,
    deviceCredentialId
)

private fun AuthChallengeWire.toContract() = BackendResponses.AuthChallenge(
    authSessionId,
    challengeBase64Url
)

private fun TokenPairWire.toDeviceSession() = BackendResponses.DeviceSession(
    accessToken,
    refreshToken,
    Instant.parse(accessTokenExpiresAt).toEpochMilliseconds()
)

private fun TokenPairWire.toRefresh() = BackendResponses.Refresh(
    accessToken,
    refreshToken,
    Instant.parse(accessTokenExpiresAt).toEpochMilliseconds()
)

private fun StudentWire.toContract() = BackendDtos.Student(
    studentId,
    displayName,
    activatedSinceEpochMillis,
    generatedActivationCode
)

private fun CourseWire.toContract() = BackendDtos.Course(id, name)

private fun AppointmentWire.toContract() = BackendDtos.Appointment(
    id,
    title,
    room,
    course,
    protokollant,
    Instant.parse(startAt),
    Instant.parse(endAt),
    moduleId,
    ignored,
    lecturerId,
    lecturerOverride,
    lecturerEditable
)

private fun AttendanceWire.toContract() = BackendDtos.Attendance(
    student.toContract(),
    AttendanceStatusDto.valueOf(status),
    lateMinutes
)

private fun AttendanceDayWire.toContract() = BackendResponses.AttendanceDay(
    appointment.toContract(),
    attendances.map { attendance -> attendance.toContract() },
    attendancesValidated
)

private fun AppointmentOverviewWire.toContract() = BackendResponses.AppointmentOverview(
    appointments.map { appointment -> appointment.toContract() },
    activeAppointmentId,
    activeAttendanceDay?.toContract()
)

private fun StudentHomeWire.toContract() = BackendResponses.StudentHomeData(
    student.toContract(),
    absences.map { absence ->
        BackendDtos.Absence(
            absence.fromEpochMillis,
            absence.untilEpochMillis,
            absence.reason,
            absence.id,
            absence.status
        )
    },
    checkInAvailability.toContract(),
    appointments.map { appointment -> appointment.toContract() }
)

private fun DozentHomeWire.toContract() = BackendResponses.DozentHomeData(appointmentOverview.toContract())

private fun CheckInAvailabilityWire.toContract() = BackendDtos.CheckInAvailability(
    state,
    appointment?.toContract(),
    opensAt?.let(Instant::parse),
    validUntil?.let(Instant::parse),
    attendanceStatus
)

private fun CourseDetailWire.toContract() = BackendResponses.CourseDetail(
    course.toContract(),
    modules.map { module -> BackendDtos.Module(module.id, module.courseGroupId, module.name, module.ignored, module.lecturerId) },
    students.map { student -> student.toContract() },
    appointments.map { appointment -> appointment.toContract() },
    attendanceDays.map { attendance -> attendance.toContract() }
)

private fun SecretaryHomeWire.toContract() = BackendResponses.VerwaltungHomeData(
    courseDetails.map { course -> course.toContract() },
    moduleEnrollments.map { enrollment ->
        BackendDtos.ModuleEnrollment(enrollment.studentId, enrollment.moduleId)
    },
    absenceReviews.map { review ->
        BackendDtos.AbsenceReview(
            review.id,
            review.student.toContract(),
            review.fromEpochMillis,
            review.untilEpochMillis,
            review.reason,
            review.status
        )
    },
    lecturers.map { BackendDtos.Lecturer(it.id, it.displayName, it.active) }
)

private fun NfcCheckInResultWire.toContract() = BackendResponses.NfcCheckInResult(
    result,
    appointmentId,
    attendanceStatus,
    validUntilEpochMillis,
    message
)

private fun String.toAppRole() = when (this) {
    "STUDENT" -> UserRole.STUDENT
    "LECTURER" -> UserRole.DOZENT
    "SECRETARY" -> UserRole.VERWALTUNG
    else -> UserRole.NONE
}
