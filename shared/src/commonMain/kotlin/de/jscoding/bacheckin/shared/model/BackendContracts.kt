package de.jscoding.bacheckin.shared.model

import kotlin.time.Instant

object BackendDtos {
    data class DeviceInfo(
        val appVersionName: String,
        val appVersionCode: Long,
        val androidSdk: Int,
        val manufacturer: String,
        val model: String
    )

    data class Student(
        val studentId: String,
        val displayName: String? = null,
        val activatedSinceEpochMillis: Long? = null,
        val generatedActivationCode: String? = null
    )

    data class Course(
        val id: String,
        val name: String
    )

    data class Module(
        val id: String,
        val courseGroupId: String,
        val name: String,
        val ignored: Boolean = false,
        val lecturerId: String? = null
    )
    data class Lecturer(val id: String, val displayName: String, val active: Boolean = true)
    data class ModuleEnrollment(val studentId: String, val moduleId: String)

    data class Appointment(
        val id: String,
        val title: String,
        val room: String,
        val course: String,
        val protokollant: String,
        val startAt: Instant,
        val endAt: Instant,
        val moduleId: String? = null,
        val ignored: Boolean = false,
        val lecturerId: String? = null,
        val lecturerOverride: Boolean = false,
        val lecturerEditable: Boolean = false
    )

    data class Absence(
        val fromEpochMillis: Long,
        val untilEpochMillis: Long,
        val reason: String? = null,
        val id: String = "",
        val status: String = "SUBMITTED"
    )

    data class CheckInAvailability(
        val state: String,
        val appointment: Appointment? = null,
        val opensAt: Instant? = null,
        val validUntil: Instant? = null,
        val attendanceStatus: String? = null
    )

    data class AbsenceReview(
        val id: String,
        val student: Student,
        val fromEpochMillis: Long,
        val untilEpochMillis: Long,
        val reason: String? = null,
        val status: String
    )

    data class Attendance(
        val student: Student,
        val status: AttendanceStatusDto,
        val lateMinutes: Int? = null
    )
}


object BackendRequests {
    data class CompleteActivation(
        val activationSessionId: String,
        val activationCode: String,
        val publicKeyBase64: String,
        val challengeSignatureBase64: String,
        val deviceInfo: BackendDtos.DeviceInfo
    )

    data class AuthChallenge(
        val deviceCredentialId: String
    )

    data class DeviceSession(
        val deviceCredentialId: String,
        val authSessionId: String,
        val timestampEpochMillis: Long,
        val signatureBase64: String
    )

    data class Refresh(
        val deviceCredentialId: String,
        val refreshToken: String,
        val timestampEpochMillis: Long,
        val signatureBase64: String
    )

    data class NfcCheckInRequest(
        val cardPublicId: String,
        val encryptedPiccDataHex: String,
        val sdmMacHex: String
    )
}


object BackendResponses {
    data class ActivationChallenge(
        val activationSessionId: String,
        val challengeBase64: String
    )

    data class CompleteActivation(
        val userId: String,
        val userRole: UserRole,
        val displayName: String?,
        val deviceId: String,
        val deviceCredentialId: String
    )

    data class AuthChallenge(
        val authSessionId: String,
        val challengeBase64: String
    )

    data class DeviceSession(
        val accessToken: String,
        val refreshToken: String?,
        val accessTokenExpiresAtEpochMillis: Long
    )

    data class Refresh(
        val accessToken: String,
        val refreshToken: String?,
        val accessTokenExpiresAtEpochMillis: Long
    )

    data class StudentHomeData(
        val student: BackendDtos.Student,
        val absences: List<BackendDtos.Absence>,
        val checkInAvailability: BackendDtos.CheckInAvailability,
        val appointments: List<BackendDtos.Appointment>
    )

    data class NfcCheckInResult(
        val result: String,
        val appointmentId: String?,
        val attendanceStatus: String?,
        val validUntilEpochMillis: Long? = null,
        val message: String? = null
    )

    data class DozentHomeData(
        val appointmentOverview: AppointmentOverview
    )

    data class VerwaltungHomeData(
        val courseDetails: List<CourseDetail>,
        val moduleEnrollments: List<BackendDtos.ModuleEnrollment> = emptyList(),
        val absenceReviews: List<BackendDtos.AbsenceReview> = emptyList(),
        val lecturers: List<BackendDtos.Lecturer> = emptyList()
    )

    data class AttendanceDay(
        val appointment: BackendDtos.Appointment,
        val attendances: List<BackendDtos.Attendance>,
        val attendancesValidated: Boolean
    )

    data class AppointmentOverview(
        val appointments: List<BackendDtos.Appointment>,
        val activeAppointmentId: String?,
        val activeAttendanceDay: AttendanceDay?
    )

    data class CourseDetail(
        val course: BackendDtos.Course,
        val modules: List<BackendDtos.Module> = emptyList(),
        val students: List<BackendDtos.Student>,
        val appointments: List<BackendDtos.Appointment>,
        val attendanceDays: List<AttendanceDay>
    )

    data class CalendarImportResult(
        val created: Int,
        val updated: Int,
        val archived: Int,
        val skipped: Int
    )
}

enum class AttendanceStatusDto {
    MISSING,
    PRESENT,
    LATE,
    PARTIALLY,
    ABSENT,
    EXCUSED,
    MANUAL_REVIEW
}

enum class UserRole {
    STUDENT,
    DOZENT,
    VERWALTUNG,
    NONE
}

enum class BackendDeviceErrorCode {
    INVALID_ACTIVATION_CODE,
    ACTIVATION_CODE_EXPIRED,
    ACTIVATION_CODE_ALREADY_USED,
    DEVICE_REVOKED,
    DEVICE_NOT_ACTIVE,
    DEVICE_REPLACED,
    DEVICE_KEY_REJECTED,
    RATE_LIMITED,
    NETWORK_ERROR,
    UNKNOWN
}

class BackendException(
    val code: BackendDeviceErrorCode,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

interface BackendRemoteDataSource {
    @Throws(Exception::class)
    suspend fun getStudentHomeData(): BackendResponses.StudentHomeData

    @Throws(Exception::class)
    suspend fun getDozentHomeData(): BackendResponses.DozentHomeData

    @Throws(Exception::class)
    suspend fun openAppointment(appointmentId: String): BackendResponses.AppointmentOverview

    @Throws(Exception::class)
    suspend fun updateStudentAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay

    @Throws(Exception::class)
    suspend fun validateAppointmentAttendances(appointmentId: String): BackendResponses.AttendanceDay

    @Throws(Exception::class)
    suspend fun selectAppointmentProtokollant(
        appointmentId: String,
        studentId: String
    ): BackendResponses.AppointmentOverview

    @Throws(Exception::class)
    suspend fun getVerwaltungHomeData(): BackendResponses.VerwaltungHomeData

    @Throws(Exception::class)
    suspend fun createVerwaltungUserActivationCode(studentId: String): BackendDtos.Student

    @Throws(Exception::class)
    suspend fun revokeVerwaltungUserDevice(studentId: String): BackendDtos.Student

    @Throws(Exception::class)
    suspend fun updateVerwaltungAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay

    @Throws(Exception::class)
    suspend fun validateVerwaltungAttendances(appointmentId: String): BackendResponses.AttendanceDay

    @Throws(Exception::class)
    suspend fun validateActivationCode(activationCode: String)

    @Throws(Exception::class)
    suspend fun createActivationChallenge(): BackendResponses.ActivationChallenge

    @Throws(Exception::class)
    suspend fun completeActivation(
        request: BackendRequests.CompleteActivation
    ): BackendResponses.CompleteActivation

    @Throws(Exception::class)
    suspend fun verifyNfcCheckIn(request: BackendRequests.NfcCheckInRequest): BackendResponses.NfcCheckInResult

    @Throws(Exception::class)
    suspend fun assignStudentModule(studentId: String, moduleId: String)

    @Throws(Exception::class)
    suspend fun removeStudentModule(studentId: String, moduleId: String)

    @Throws(Exception::class)
    suspend fun updateLectureModule(appointmentId: String, moduleId: String)

    @Throws(Exception::class)
    suspend fun updateVerwaltungAppointmentIgnored(appointmentId: String, ignored: Boolean)

    @Throws(Exception::class)
    suspend fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean)

    @Throws(Exception::class)
    suspend fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?)

    @Throws(Exception::class)
    suspend fun updateVerwaltungAppointmentLecturer(appointmentId: String, overrideModule: Boolean, lecturerId: String?)

    @Throws(Exception::class)
    suspend fun reviewAbsence(absenceId: String, status: String)

    @Throws(Exception::class)
    suspend fun createAuthChallenge(
        request: BackendRequests.AuthChallenge
    ): BackendResponses.AuthChallenge

    @Throws(Exception::class)
    suspend fun openDeviceSession(
        request: BackendRequests.DeviceSession
    ): BackendResponses.DeviceSession

    @Throws(Exception::class)
    suspend fun refresh(request: BackendRequests.Refresh): BackendResponses.Refresh

    @Throws(Exception::class)
    suspend fun logoutDevice(deviceCredentialId: String)

    @Throws(Exception::class)
    suspend fun unlinkCurrentDevice()

    @Throws(Exception::class)
    suspend fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long)

    @Throws(Exception::class)
    suspend fun importTrainexCalendar(
        sourceKey: String,
        icsContent: String
    ): BackendResponses.CalendarImportResult
}
