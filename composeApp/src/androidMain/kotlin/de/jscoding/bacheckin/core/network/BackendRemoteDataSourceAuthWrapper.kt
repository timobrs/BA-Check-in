package de.jscoding.bacheckin.core.network

import de.jscoding.bacheckin.core.session.TokenManager
import de.jscoding.bacheckin.shared.dataSource.BackendRemoteDataSourceImpl
import de.jscoding.bacheckin.shared.model.AttendanceStatusDto
import de.jscoding.bacheckin.shared.model.BackendDtos
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import de.jscoding.bacheckin.shared.model.BackendRequests
import de.jscoding.bacheckin.shared.model.BackendResponses
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendRemoteDataSourceAuthWrapper @Inject constructor(
    private val tokenManager: TokenManager,
    private val delegate: BackendRemoteDataSourceImpl
) : BackendRemoteDataSource {
    private suspend fun <T> withValidToken(block: suspend () -> T): T {
        val accessToken = tokenManager.getValidAccessToken()
        delegate.setAccessToken(accessToken)
        return block()
    }

    override suspend fun getStudentHomeData(): BackendResponses.StudentHomeData =
        withValidToken { delegate.getStudentHomeData() }

    override suspend fun getDozentHomeData(): BackendResponses.DozentHomeData =
        withValidToken { delegate.getDozentHomeData() }

    override suspend fun openAppointment(appointmentId: String): BackendResponses.AppointmentOverview =
        withValidToken { delegate.openAppointment(appointmentId) }

    override suspend fun updateStudentAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay =
        withValidToken { delegate.updateStudentAttendance(appointmentId, studentId, status) }

    override suspend fun validateAppointmentAttendances(appointmentId: String): BackendResponses.AttendanceDay =
        withValidToken { delegate.validateAppointmentAttendances(appointmentId) }

    override suspend fun selectAppointmentProtokollant(
        appointmentId: String,
        studentId: String
    ): BackendResponses.AppointmentOverview =
        withValidToken { delegate.selectAppointmentProtokollant(appointmentId, studentId) }

    override suspend fun getVerwaltungHomeData(): BackendResponses.VerwaltungHomeData =
        withValidToken { delegate.getVerwaltungHomeData() }

    override suspend fun createVerwaltungUserActivationCode(studentId: String): BackendDtos.Student =
        withValidToken { delegate.createVerwaltungUserActivationCode(studentId) }

    override suspend fun revokeVerwaltungUserDevice(studentId: String): BackendDtos.Student =
        withValidToken { delegate.revokeVerwaltungUserDevice(studentId) }

    override suspend fun updateVerwaltungAttendance(
        appointmentId: String,
        studentId: String,
        status: AttendanceStatusDto
    ): BackendResponses.AttendanceDay =
        withValidToken { delegate.updateVerwaltungAttendance(appointmentId, studentId, status) }

    override suspend fun validateVerwaltungAttendances(appointmentId: String): BackendResponses.AttendanceDay =
        withValidToken { delegate.validateVerwaltungAttendances(appointmentId) }

    override suspend fun validateActivationCode(activationCode: String) =
        delegate.validateActivationCode(activationCode)

    override suspend fun createActivationChallenge(): BackendResponses.ActivationChallenge =
        delegate.createActivationChallenge()

    override suspend fun completeActivation(
        request: BackendRequests.CompleteActivation
    ): BackendResponses.CompleteActivation = delegate.completeActivation(request)

    override suspend fun createAuthChallenge(
        request: BackendRequests.AuthChallenge
    ): BackendResponses.AuthChallenge = delegate.createAuthChallenge(request)

    override suspend fun openDeviceSession(
        request: BackendRequests.DeviceSession
    ): BackendResponses.DeviceSession = delegate.openDeviceSession(request)

    override suspend fun refresh(request: BackendRequests.Refresh): BackendResponses.Refresh =
        delegate.refresh(request)

    override suspend fun logoutDevice(deviceCredentialId: String) = withValidToken {
        delegate.logoutDevice(deviceCredentialId)
    }

    override suspend fun unlinkCurrentDevice() = withValidToken { delegate.unlinkCurrentDevice() }

    override suspend fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long) =
        withValidToken {
            delegate.reportAbsence(fromEpochMillis, untilEpochMillis)
        }


    override suspend fun importTrainexCalendar(
        sourceKey: String,
        icsContent: String
    ): BackendResponses.CalendarImportResult = withValidToken {
        delegate.importTrainexCalendar(sourceKey, icsContent)
    }

    override suspend fun verifyNfcCheckIn(
        request: BackendRequests.NfcCheckInRequest
    ): BackendResponses.NfcCheckInResult = withValidToken {
        delegate.verifyNfcCheckIn(request)
    }

    override suspend fun assignStudentModule(studentId: String, moduleId: String) =
        withValidToken { delegate.assignStudentModule(studentId, moduleId) }

    override suspend fun removeStudentModule(studentId: String, moduleId: String) =
        withValidToken { delegate.removeStudentModule(studentId, moduleId) }

    override suspend fun updateLectureModule(appointmentId: String, moduleId: String) =
        withValidToken { delegate.updateLectureModule(appointmentId, moduleId) }

    override suspend fun updateVerwaltungAppointmentIgnored(appointmentId: String, ignored: Boolean) =
        withValidToken { delegate.updateVerwaltungAppointmentIgnored(appointmentId, ignored) }

    override suspend fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean) =
        withValidToken { delegate.updateVerwaltungModuleIgnored(moduleId, ignored) }

    override suspend fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?) =
        withValidToken { delegate.updateVerwaltungModuleLecturer(moduleId, lecturerId) }

    override suspend fun updateVerwaltungAppointmentLecturer(appointmentId: String, overrideModule: Boolean, lecturerId: String?) =
        withValidToken { delegate.updateVerwaltungAppointmentLecturer(appointmentId, overrideModule, lecturerId) }

    override suspend fun reviewAbsence(absenceId: String, status: String) =
        withValidToken { delegate.reviewAbsence(absenceId, status) }
}
