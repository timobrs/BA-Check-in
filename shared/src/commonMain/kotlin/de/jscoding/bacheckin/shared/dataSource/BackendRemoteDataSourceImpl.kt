package de.jscoding.bacheckin.shared.dataSource

import de.jscoding.bacheckin.shared.model.AttendanceStatusDto
import de.jscoding.bacheckin.shared.model.BackendAccessTokenReceiver
import de.jscoding.bacheckin.shared.model.BackendDtos
import de.jscoding.bacheckin.shared.model.BackendMode
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import de.jscoding.bacheckin.shared.model.BackendRequests
import de.jscoding.bacheckin.shared.model.BackendResponses
import de.jscoding.bacheckin.shared.model.BackendRuntimeConfig

class BackendRemoteDataSourceImpl(
    config: BackendRuntimeConfig,
    postgresBackendRemoteDataSource: PostgresBackendRemoteDataSource
) : BackendRemoteDataSource, BackendAccessTokenReceiver {

    private val demoBackendRemoteDataSource = DemoBackendRemoteDataSource()
    private val activeBackend: BackendRemoteDataSource = when (config.mode) {
        BackendMode.DEMO -> demoBackendRemoteDataSource
        BackendMode.POSTGRES_API -> postgresBackendRemoteDataSource
    }

    override fun setAccessToken(accessToken: String?) {
        (activeBackend as? BackendAccessTokenReceiver)?.setAccessToken(accessToken)
    }

    override suspend fun getStudentHomeData(): BackendResponses.StudentHomeData {
        return activeBackend.getStudentHomeData()
    }

    override suspend fun getDozentHomeData(): BackendResponses.DozentHomeData {
        return activeBackend.getDozentHomeData()
    }

    override suspend fun openAppointment(appointmentId: String): BackendResponses.AppointmentOverview {
        return activeBackend.openAppointment(appointmentId)
    }

    override suspend fun updateStudentAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay {
        return activeBackend.updateStudentAttendance(
            appointmentId = appointmentId,
            studentId = studentId,
            status = status
        )
    }

    override suspend fun validateAppointmentAttendances(
        appointmentId: String
    ): BackendResponses.AttendanceDay {
        return activeBackend.validateAppointmentAttendances(appointmentId)
    }

    override suspend fun selectAppointmentProtokollant(
        appointmentId: String,
        studentId: String
    ): BackendResponses.AppointmentOverview {
        return activeBackend.selectAppointmentProtokollant(
            appointmentId = appointmentId,
            studentId = studentId
        )
    }

    override suspend fun getVerwaltungHomeData(): BackendResponses.VerwaltungHomeData {
        return activeBackend.getVerwaltungHomeData()
    }

    override suspend fun createVerwaltungUserActivationCode(studentId: String): BackendDtos.Student {
        return activeBackend.createVerwaltungUserActivationCode(studentId)
    }

    override suspend fun revokeVerwaltungUserDevice(studentId: String): BackendDtos.Student {
        return activeBackend.revokeVerwaltungUserDevice(studentId)
    }

    override suspend fun updateVerwaltungAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay {
        return activeBackend.updateVerwaltungAttendance(appointmentId, studentId, status)
    }

    override suspend fun validateVerwaltungAttendances(
        appointmentId: String
    ): BackendResponses.AttendanceDay {
        return activeBackend.validateVerwaltungAttendances(appointmentId)
    }

    override suspend fun validateActivationCode(activationCode: String) {
        activeBackend.validateActivationCode(activationCode)
    }

    override suspend fun createActivationChallenge(): BackendResponses.ActivationChallenge {
        return activeBackend.createActivationChallenge()
    }

    override suspend fun completeActivation(
        request: BackendRequests.CompleteActivation
    ): BackendResponses.CompleteActivation {
        return activeBackend.completeActivation(request)
    }

    override suspend fun createAuthChallenge(
        request: BackendRequests.AuthChallenge
    ): BackendResponses.AuthChallenge {
        return activeBackend.createAuthChallenge(request)
    }

    override suspend fun openDeviceSession(
        request: BackendRequests.DeviceSession
    ): BackendResponses.DeviceSession {
        return activeBackend.openDeviceSession(request)
    }

    override suspend fun refresh(request: BackendRequests.Refresh): BackendResponses.Refresh {
        return activeBackend.refresh(request)
    }

    override suspend fun logoutDevice(deviceCredentialId: String) {
        activeBackend.logoutDevice(deviceCredentialId)
    }

    override suspend fun unlinkCurrentDevice() {
        activeBackend.unlinkCurrentDevice()
    }

    override suspend fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long) {
        activeBackend.reportAbsence(fromEpochMillis, untilEpochMillis)
    }

    override suspend fun importTrainexCalendar(
        sourceKey: String,
        icsContent: String
    ): BackendResponses.CalendarImportResult = activeBackend.importTrainexCalendar(sourceKey, icsContent)

    override suspend fun verifyNfcCheckIn(request: BackendRequests.NfcCheckInRequest): BackendResponses.NfcCheckInResult {
        return activeBackend.verifyNfcCheckIn(request)
    }

    override suspend fun assignStudentModule(studentId: String, moduleId: String) =
        activeBackend.assignStudentModule(studentId, moduleId)

    override suspend fun removeStudentModule(studentId: String, moduleId: String) =
        activeBackend.removeStudentModule(studentId, moduleId)

    override suspend fun updateLectureModule(appointmentId: String, moduleId: String) =
        activeBackend.updateLectureModule(appointmentId, moduleId)

    override suspend fun updateVerwaltungAppointmentIgnored(appointmentId: String, ignored: Boolean) =
        activeBackend.updateVerwaltungAppointmentIgnored(appointmentId, ignored)

    override suspend fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean) =
        activeBackend.updateVerwaltungModuleIgnored(moduleId, ignored)

    override suspend fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?) =
        activeBackend.updateVerwaltungModuleLecturer(moduleId, lecturerId)

    override suspend fun updateVerwaltungAppointmentLecturer(appointmentId: String, overrideModule: Boolean, lecturerId: String?) =
        activeBackend.updateVerwaltungAppointmentLecturer(appointmentId, overrideModule, lecturerId)

    override suspend fun reviewAbsence(absenceId: String, status: String) =
        activeBackend.reviewAbsence(absenceId, status)
}
