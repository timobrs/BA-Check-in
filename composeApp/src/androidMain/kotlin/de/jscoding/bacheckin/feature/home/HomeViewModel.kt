package de.jscoding.bacheckin.feature.home

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.jscoding.bacheckin.BuildConfig
import de.jscoding.bacheckin.core.device.ObserveDeviceBindingUseCase
import de.jscoding.bacheckin.core.network.NetworkMonitor
import de.jscoding.bacheckin.core.nfc.NfcReader
import de.jscoding.bacheckin.core.nfc.Ntag424DnaHandler
import de.jscoding.bacheckin.core.session.InactivityDetector
import de.jscoding.bacheckin.core.session.TokenManager
import de.jscoding.bacheckin.shared.model.AttendanceStatusDto
import de.jscoding.bacheckin.shared.model.BackendDtos
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import de.jscoding.bacheckin.shared.model.BackendRequests
import de.jscoding.bacheckin.shared.model.BackendResponses
import de.jscoding.bacheckin.shared.model.UserRole
import de.jscoding.bacheckin.shared.trainexImporter.TrainexImporter
import de.jscoding.bacheckin.shared.trainexImporter.getTrainexAccounts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class HomeUiState(
    val userRole: UserRole = UserRole.NONE,
    val userDisplayName: String? = null,
    val isOnline: Boolean = true,
    val activeSheet: HomeSheet? = null,

    val selectedDay: Day = Day.Today,
    val appointments: List<Appointment> = emptyList(),
    val activeAppointment: Appointment? = null,

    val studentId: String? = null,
    val studentName: String? = null,
    val checkInState: CheckInState = CheckInState.Unavailable,
    val absences: List<ReportedAbsence> = emptyList(),

    val studentCount: Int = 0,
    val studentAttendCount: Int = 0,
    val attendances: List<StudentAttendance> = emptyList(),
    val attendanceEditorStatusOverrides: Map<String, AttendanceStatus> = emptyMap(),
    val attendanceEditorDisplayedStatusOverrides: Map<String, AttendanceStatus> = emptyMap(),
    val pendingAttendanceStudentIds: Set<String> = emptySet(),
    val attendancesValidated: Boolean = false,

    val verwaltungSearchQuery: String = "",
    val verwaltungCourses: List<VerwaltungCourse> = emptyList(),
    val verwaltungCourseDetails: Map<String, VerwaltungCourseDetail> = emptyMap(),
    val verwaltungAttendanceDay: VerwaltungAttendanceDay? = null,
    val verwaltungAbsenceReviews: List<VerwaltungAbsenceReview> = emptyList(),
    val refreshIsLoading: Boolean = false,
)

sealed interface CheckInState {
    data class Absent(
        val untilAt: Instant? = null,
        val isFullDay: Boolean = true
    ) : CheckInState

    data object Failed : CheckInState
    data class Success(val untilAt: Instant) : CheckInState
    data class Scanning(val nfcId: String? = null) : CheckInState
    data class Waiting(val message: String) : CheckInState
    data object Unavailable : CheckInState
}

data class Appointment(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val room: String,
    val startAt: Instant,
    val endAt: Instant,
    val course: String,
    val protokollant: String,
    val moduleId: String? = null,
    val ignored: Boolean = false,
    val lecturerId: String? = null,
    val lecturerOverride: Boolean = false,
    val lecturerEditable: Boolean = false,
)

data class StudentAttendance(
    val studentId: String,
    val displayName: String,
    val status: AttendanceStatus
)

data class VerwaltungCourse(
    val id: String,
    val name: String
)

data class VerwaltungModule(
    val id: String,
    val courseGroupId: String,
    val name: String,
    val ignored: Boolean = false,
    val lecturerId: String? = null
)
data class VerwaltungAbsenceReview(
    val id: String,
    val studentName: String,
    val fromAt: Instant,
    val untilAt: Instant
)

data class VerwaltungUser(
    val studentId: String,
    val displayName: String,
    val activatedSince: Instant?,
    val generatedActivationCode: String?
)

data class VerwaltungAttendanceDay(
    val appointment: Appointment,
    val attendances: List<StudentAttendance>,
    val attendancesValidated: Boolean
)

data class VerwaltungCourseDetail(
    val course: VerwaltungCourse,
    val users: List<VerwaltungUser>,
    val appointments: List<Appointment>,
    val attendanceDays: List<VerwaltungAttendanceDay>,
    val availableModules: List<VerwaltungModule> = emptyList(),
    val moduleEnrollments: Map<String, Set<String>> = emptyMap(),
    val lecturers: List<BackendDtos.Lecturer> = emptyList()
)

data class VerwaltungUserAttendanceDay(
    val appointment: Appointment,
    val status: AttendanceStatus,
    val lateMinutes: Int? = null
)

data class VerwaltungUserDetail(
    val user: VerwaltungUser,
    val attendanceDays: List<VerwaltungUserAttendanceDay>,
    val availableModules: List<VerwaltungModule> = emptyList(),
    val assignedModuleIds: Set<String> = emptySet()
)

data class ReportedAbsence(
    val id: String,
    val fromAt: Instant,
    val untilAt: Instant,
    val status: String
)

enum class AttendanceStatus {
    Missing,
    Present,
    Late,
    Partially,
    Absent,
    Excused,
    ManualReview
}

enum class Day {
    Today, Tomorrow
}

enum class HomeSheet {
    Absence,
    Attendances,
    AttendanceEditor,
    AttendanceConfirmationEditor,
    VerwaltungAttendances,
    VerwaltungAttendanceEditor
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDeviceBindingUseCase: ObserveDeviceBindingUseCase,
    networkMonitor: NetworkMonitor,
    private val backendRemoteDataSource: BackendRemoteDataSource,
    private val tokenManager: TokenManager,
    private val inactivityDetector: InactivityDetector
) : ViewModel() {

    private val _internalState = MutableStateFlow(HomeInternalState())
    private val _refreshTrigger = MutableStateFlow(0)

    private val _deviceBindingState = observeDeviceBindingUseCase()

    private val _backendHomeState = _deviceBindingState.flatMapLatest { binding ->
        _refreshTrigger.flatMapLatest {
            if (binding.hasActiveLocalBinding) {
                flow {
                    val studentHomeData = if (binding.userRole == UserRole.STUDENT) {
                        runCatching { backendRemoteDataSource.getStudentHomeData() }.getOrNull()
                    } else {
                        null
                    }
                    val dozentHomeData = if (binding.userRole == UserRole.DOZENT) {
                        runCatching { backendRemoteDataSource.getDozentHomeData() }.getOrNull()
                    } else {
                        null
                    }
                    val verwaltungHomeData = if (binding.userRole == UserRole.VERWALTUNG) {
                        runCatching { backendRemoteDataSource.getVerwaltungHomeData() }.getOrNull()
                    } else {
                        null
                    }

                    if (binding.userRole == UserRole.VERWALTUNG && verwaltungHomeData == null) {
                        return@flow
                    }

                    emit(
                        HomeBackendData(
                            studentHomeData = studentHomeData,
                            dozentHomeData = dozentHomeData,
                            verwaltungHomeData = verwaltungHomeData
                        )
                    )
                }
            } else {
                flow { emit(HomeBackendData()) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeBackendData()
    )

    init {
        viewModelScope.launch {
            _backendHomeState.collectLatest { data ->
                val availability = data.studentHomeData?.checkInAvailability ?: return@collectLatest
                val nextBoundary = listOfNotNull(
                    availability.opensAt,
                    availability.appointment?.startAt,
                    availability.validUntil
                ).map { it.toEpochMilliseconds() }
                    .filter { it > System.currentTimeMillis() }
                    .minOrNull() ?: return@collectLatest
                delay(((nextBoundary - System.currentTimeMillis()).coerceAtLeast(1L) + 100L).milliseconds)
                _refreshTrigger.value++
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _internalState,
        _deviceBindingState,
        networkMonitor.isOnline,
        _backendHomeState
    ) { state, deviceBinding, isOnline, backendHomeData ->
        val reportedAbsences = backendHomeData.studentHomeData?.absences
            ?.map { it.toReportedAbsence() }
            .orEmpty()
        val now = Instant.now()
        val activeAbsences = reportedAbsences.filter { it.status != "REJECTED" }
        val currentAbsence = activeAbsences.firstOrNull { it.contains(now) }
        val availability = backendHomeData.studentHomeData?.checkInAvailability
        val checkInUntilAt = availability?.validUntil
            ?.let { Instant.ofEpochMilli(it.toEpochMilliseconds()) }
        val nextAbsenceStartAt = checkInUntilAt?.let { untilAt ->
            activeAbsences
                .filter { it.fromAt.isAfter(now) && it.fromAt.isBefore(untilAt) }
                .minByOrNull { it.fromAt }
                ?.fromAt
        }
        val effectiveCheckInState = when {
            currentAbsence != null -> CheckInState.Absent(
                untilAt = currentAbsence.untilAt,
                isFullDay = currentAbsence.isFullDay()
            )

            availability?.state == "ACTIVE" && checkInUntilAt != null -> CheckInState.Success(
                untilAt = nextAbsenceStartAt ?: checkInUntilAt
            )

            availability?.state == "AVAILABLE" -> if (state.checkInState is CheckInState.Failed) {
                state.checkInState
            } else {
                CheckInState.Scanning()
            }

            availability?.state == "TOO_EARLY" -> CheckInState.Waiting(
                availability.opensAt?.let {
                    "Check-in möglich ab ${
                        Instant.ofEpochMilli(it.toEpochMilliseconds())
                            .atZone(ZoneId.systemDefault()).toLocalTime()
                    }"
                }
                    ?: "Der nächste Check-in ist noch nicht freigeschaltet."
            )

            availability?.state == "OVERLAPPING_LECTURES" -> CheckInState.Waiting(
                "Mehrere Veranstaltungen überschneiden sich. Bitte die Verwaltung kontaktieren."
            )

            availability?.state == "NO_UPCOMING_LECTURE" -> CheckInState.Waiting(
                "Keine kommende berechtigte Veranstaltung gefunden."
            )

            else -> CheckInState.Unavailable
        }
        val dozentOverview = backendHomeData.dozentHomeData?.appointmentOverview
        val appointments = when (deviceBinding.userRole) {
            UserRole.STUDENT -> backendHomeData.studentHomeData?.appointments.orEmpty()
            UserRole.DOZENT -> dozentOverview?.appointments.orEmpty()
            UserRole.VERWALTUNG -> backendHomeData.verwaltungHomeData
                ?.courseDetails
                ?.flatMap { it.appointments }
                ?.distinctBy { it.id }
                .orEmpty()

            UserRole.NONE -> emptyList()
        }.map { it.toAppointment() }
        val activeAppointment = dozentOverview
            ?.activeAppointmentId
            ?.let { activeId -> appointments.firstOrNull { it.id.toString() == activeId } }
        val activeAttendanceDay = dozentOverview?.activeAttendanceDay
        val attendances = activeAttendanceDay
            ?.attendances
            ?.map { it.toStudentAttendance() }
            .orEmpty()
        val verwaltungData = backendHomeData.verwaltungHomeData
        val allModules =
            verwaltungData?.courseDetails.orEmpty().flatMap { it.modules }.distinctBy { it.id }
        val enrollmentsByStudent = verwaltungData?.moduleEnrollments.orEmpty()
            .groupBy { it.studentId }
            .mapValues { (_, values) -> values.map { it.moduleId }.toSet() }
        val verwaltungCourseDetails = verwaltungData
            ?.courseDetails
            ?.associate { detail ->
                detail.course.id to detail.toVerwaltungCourseDetail(
                    allModules,
                    enrollmentsByStudent,
                    state.verwaltungActivationCodes,
                    verwaltungData.lecturers
                )
            }
            .orEmpty()
        val verwaltungAttendanceDay = state.verwaltungAttendanceCourseId
            ?.let { verwaltungCourseDetails[it] }
            ?.attendanceDays
            ?.firstOrNull { it.appointment.id == state.selectedVerwaltungAppointmentId }

        HomeUiState(
            userRole = deviceBinding.userRole,
            userDisplayName = deviceBinding.displayName,
            isOnline = isOnline,
            studentId = backendHomeData.studentHomeData?.student?.studentId,
            studentName = backendHomeData.studentHomeData?.student?.displayName,
            checkInState = effectiveCheckInState,
            absences = reportedAbsences,
            appointments = appointments,
            activeAppointment = activeAppointment,
            selectedDay = state.selectedDay,
            activeSheet = state.activeSheet,
            studentCount = attendances.size,
            studentAttendCount = attendances.count {
                it.status == AttendanceStatus.Present || it.status == AttendanceStatus.Late
            },
            attendances = attendances,
            attendanceEditorStatusOverrides = state.attendanceEditorStatusOverrides,
            attendanceEditorDisplayedStatusOverrides = state.attendanceEditorDisplayedStatusOverrides,
            pendingAttendanceStudentIds = state.pendingAttendanceStudentIds,
            attendancesValidated = activeAttendanceDay?.attendancesValidated ?: false,
            verwaltungSearchQuery = state.verwaltungSearchQuery,
            verwaltungCourses = verwaltungCourseDetails.values.map { it.course },
            verwaltungCourseDetails = verwaltungCourseDetails,
            verwaltungAttendanceDay = verwaltungAttendanceDay,
            verwaltungAbsenceReviews = verwaltungData?.absenceReviews.orEmpty().map {
                VerwaltungAbsenceReview(
                    it.id,
                    it.student.displayName ?: it.student.studentId,
                    Instant.ofEpochMilli(it.fromEpochMillis),
                    Instant.ofEpochMilli(it.untilEpochMillis)
                )
            },
            refreshIsLoading = state.refreshIsLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents

    val inactivityRemaining = inactivityDetector.remainingFlow

    fun manualRefresh() {
        inactivityDetector.markInteraction()
        viewModelScope.launch {
            try {
                _uiEvents.emit(UiEvent.ShowSpinner(true))
                tokenManager.refreshNow()
                refreshBackendHomeData()
                _uiEvents.emit(UiEvent.ShowSpinner(false))
                _uiEvents.emit(UiEvent.ShowMessage("Session verlängert"))
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowSpinner(false))
                _uiEvents.emit(UiEvent.ShowMessage("Session konnte nicht verlängert werden: ${error.message}"))
            }
        }
    }

    private var nfcReader: NfcReader? = null

    fun startNfcCheckIn(activity: FragmentActivity) {
        if (nfcReader != null) return
        viewModelScope.launch {
            try {
                if (uiState.value.checkInState !is CheckInState.Scanning) {
                    setCheckInState(CheckInState.Unavailable)
                    return@launch
                }
                val nfcTagReader = NfcReader(activity)
                nfcReader = nfcTagReader
                if (!nfcTagReader.start(onTagDiscovered = { discoveredTag ->
                        try {
                            val ntagHandlerMode = if (BuildConfig.BACKEND_MODE == "DEMO") {
                                Ntag424DnaHandler.Mode.DEMO
                            } else {
                                Ntag424DnaHandler.Mode.REAL
                            }
                            val tagScanData = Ntag424DnaHandler(ntagHandlerMode)
                                .readTagScanData(discoveredTag)
                            val checkInResult = backendRemoteDataSource.verifyNfcCheckIn(
                                BackendRequests.NfcCheckInRequest(
                                    tagScanData.cardPublicId,
                                    tagScanData.encryptedPiccDataHex,
                                    tagScanData.sdmMacHex
                                )
                            )
                            stopNfcCheckIn()
                            val checkInValidUntil = checkInResult.validUntilEpochMillis
                            if (checkInResult.result == "CHECKED_IN" && checkInValidUntil != null) {
                                setCheckInState(
                                    CheckInState.Success(
                                        Instant.ofEpochMilli(
                                            checkInValidUntil
                                        )
                                    )
                                )
                                _refreshTrigger.value++
                            } else {
                                setCheckInState(CheckInState.Failed)
                                checkInResult.message?.let { _uiEvents.emit(UiEvent.ShowMessage(it)) }
                                refreshBackendHomeData()
                            }
                        } catch (error: Exception) {
                            stopNfcCheckIn()
                            setCheckInState(CheckInState.Failed)
                            _uiEvents.emit(UiEvent.ShowMessage("NFC-Check-in fehlgeschlagen: ${error.message}"))
                        }
                    })) {
                    nfcReader = null
                    setCheckInState(CheckInState.Failed)
                    _uiEvents.emit(UiEvent.ShowMessage("NFC ist nicht verfügbar oder deaktiviert. Bitte die NFC-Einstellungen des Geräts prüfen."))
                }
            } catch (error: Exception) {
                nfcReader = null
                setCheckInState(CheckInState.Failed)
                _uiEvents.emit(UiEvent.ShowMessage("NFC-Check-in konnte nicht gestartet werden: ${error.message}"))
            }
        }
    }

    fun stopNfcCheckIn() {
        nfcReader?.stop()
        nfcReader = null
    }

    fun refreshBackendHomeData() {
        _refreshTrigger.value++
    }

    fun retryNfcCheckIn() {
        setCheckInState(CheckInState.Scanning())
        refreshBackendHomeData()
    }

    override fun onCleared() {
        stopNfcCheckIn()
        super.onCleared()
    }

    fun markInteraction() {
        inactivityDetector.markInteraction()
    }

    fun refreshHomeData() {
        _refreshTrigger.value++
        viewModelScope.launch {
            _internalState.update { it.copy(refreshIsLoading = true) }

            try {
                // Das Backend erhält Kalenderdaten, keine Trainex-Zugangsdaten.
                val trainexUrl = "https://www.trainex36.de/ba-rm-trainex/"
                val importer = TrainexImporter(trainexUrl)
                val accounts = getTrainexAccounts()
                val calendars = importer.downloadCalendars(accounts)
                val imports = accounts.zip(calendars).mapIndexed { index, (account, calendar) ->
                    val sourceKey = MessageDigest.getInstance("SHA-256")
                        .digest(account.username.trim().lowercase().toByteArray())
                        .joinToString("") { byte -> "%02x".format(byte) }
                    try {
                        backendRemoteDataSource.importTrainexCalendar(sourceKey, calendar)
                    } catch (error: Exception) {
                        throw IllegalStateException("Trainex-Konto ${index + 1}: ${error.message}", error)
                    }
                }
                _refreshTrigger.value++
                _uiEvents.emit(
                    UiEvent.ShowMessage(
                        "Terminkalender aktualisiert: ${imports.sumOf { it.created }} neu, " +
                                "${imports.sumOf { it.updated }} aktualisiert, " +
                                "${imports.sumOf { it.archived }} archiviert, " +
                                "${imports.sumOf { it.skipped }} übersprungen"
                    )
                )
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Aktualisieren fehlgeschlagen: ${error.message}"))
            } finally {
                _internalState.update { it.copy(refreshIsLoading = false) }
            }
        }
    }

    sealed interface UiEvent {
        data class ShowMessage(val message: String) : UiEvent
        data class ShowSpinner(val show: Boolean) : UiEvent
    }

    fun selectDay(day: Day) {
        _internalState.update { it.copy(selectedDay = day) }
    }

    fun setCheckInState(state: CheckInState) {
        _internalState.update { it.copy(checkInState = state) }
    }

    fun openAbwesenheitSheet() {
        _internalState.update { it.copy(activeSheet = HomeSheet.Absence) }
    }

    fun reportAbsence(fromEpochMillis: Long, untilEpochMillis: Long) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.reportAbsence(fromEpochMillis, untilEpochMillis)
                _refreshTrigger.value++
                closeSheet()
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Abwesenheit konnte nicht gespeichert werden: ${error.message}"))
            }
        }
    }

    fun openAppointment(appointmentId: UUID) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.openAppointment(appointmentId.toString())
                _internalState.update { it.copy(activeSheet = HomeSheet.Attendances) }
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Termin konnte nicht geöffnet werden: ${error.message}"))
            }
        }
    }

    fun openAttendancesEditSheet() {
        _internalState.update { it.copy(activeSheet = HomeSheet.AttendanceEditor) }
    }

    fun acceptAppointment() {
        _internalState.update { it.copy(activeSheet = HomeSheet.AttendanceConfirmationEditor) }
    }

    fun selectAppointmentProtokollant(appointmentId: UUID, studentId: String) {
        viewModelScope.launch {
            backendRemoteDataSource.selectAppointmentProtokollant(
                appointmentId = appointmentId.toString(),
                studentId = studentId
            )
            _refreshTrigger.value++
        }
    }

    fun updateStudentAttendance(
        appointmentId: UUID,
        studentId: String,
        status: AttendanceStatus
    ) {
        _internalState.update {
            it.copy(
                attendanceEditorStatusOverrides = it.attendanceEditorStatusOverrides + (studentId to status)
            )
        }

        viewModelScope.launch {
            delay(SWITCH_ANIMATION_DELAY_MILLIS.milliseconds)
            _internalState.update {
                it.copy(pendingAttendanceStudentIds = it.pendingAttendanceStudentIds + studentId)
            }

            runCatching {
                backendRemoteDataSource.updateStudentAttendance(
                    appointmentId = appointmentId.toString(),
                    studentId = studentId,
                    status = status.toDto()
                )
            }.onSuccess {
                _internalState.update {
                    it.copy(
                        pendingAttendanceStudentIds = it.pendingAttendanceStudentIds - studentId,
                        attendanceEditorDisplayedStatusOverrides =
                            it.attendanceEditorDisplayedStatusOverrides + (studentId to status)
                    )
                }
                _refreshTrigger.value++
            }.onFailure {
                _internalState.update {
                    it.copy(
                        pendingAttendanceStudentIds = it.pendingAttendanceStudentIds - studentId,
                        attendanceEditorStatusOverrides =
                            it.attendanceEditorStatusOverrides - studentId
                    )
                }
            }
        }
    }

    fun validateAppointmentAttendances(appointmentId: UUID) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.validateAppointmentAttendances(appointmentId.toString())
                _refreshTrigger.value++
                closeSheet()
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Anwesenheiten konnten nicht bestätigt werden: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungSearchQuery(query: String) {
        _internalState.update { it.copy(verwaltungSearchQuery = query) }
    }

    fun openVerwaltungAttendances(courseId: String, appointmentId: UUID) {
        _internalState.update {
            it.copy(
                verwaltungAttendanceCourseId = courseId,
                selectedVerwaltungAppointmentId = appointmentId,
                activeSheet = HomeSheet.VerwaltungAttendances
            )
        }
    }

    fun openVerwaltungAttendanceEditSheet() {
        _internalState.update { it.copy(activeSheet = HomeSheet.VerwaltungAttendanceEditor) }
    }

    fun createVerwaltungUserActivationCode(studentId: String) {
        viewModelScope.launch {
            try {
                val updatedUser = backendRemoteDataSource.createVerwaltungUserActivationCode(studentId)
                val activationCode = updatedUser.generatedActivationCode
                if (!activationCode.isNullOrBlank()) {
                    _internalState.update {
                        it.copy(
                            verwaltungActivationCodes =
                                it.verwaltungActivationCodes + (studentId to activationCode)
                        )
                    }
                }
                _refreshTrigger.value++
                _uiEvents.emit(
                    UiEvent.ShowMessage(
                        activationCode?.let { "Freischaltcode erzeugt: $it" }
                            ?: "Freischaltcode erzeugt, aber nicht empfangen"
                    )
                )
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Freischaltcode konnte nicht erzeugt werden: ${error.message}"))
            }
        }
    }

    fun revokeVerwaltungUserDevice(studentId: String) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.revokeVerwaltungUserDevice(studentId)
                _refreshTrigger.value++
                _uiEvents.emit(UiEvent.ShowMessage("Gerät gesperrt"))
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Gerät konnte nicht gesperrt werden: ${error.message}"))
            }
        }
    }

    fun setStudentModule(studentId: String, moduleId: String, assigned: Boolean) {
        viewModelScope.launch {
            try {
                if (assigned) backendRemoteDataSource.assignStudentModule(studentId, moduleId)
                else backendRemoteDataSource.removeStudentModule(studentId, moduleId)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Modulzuordnung konnte nicht geändert werden: ${error.message}"))
            }
        }
    }

    fun reviewAbsence(absenceId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.reviewAbsence(
                    absenceId,
                    if (approved) "APPROVED" else "REJECTED"
                )
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Abwesenheit konnte nicht geprüft werden: ${error.message}"))
            }
        }
    }

    @Suppress("Unused")
    fun setLectureModule(appointmentId: UUID, moduleId: String) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.updateLectureModule(appointmentId.toString(), moduleId)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Terminmodul konnte nicht geändert werden: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungAppointmentIgnored(appointmentId: UUID, ignored: Boolean) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.updateVerwaltungAppointmentIgnored(appointmentId.toString(), ignored)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Termin konnte nicht aktualisiert werden: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungModuleIgnored(moduleId: String, ignored: Boolean) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.updateVerwaltungModuleIgnored(moduleId, ignored)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Modul konnte nicht aktualisiert werden: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungModuleLecturer(moduleId: String, lecturerId: String?) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.updateVerwaltungModuleLecturer(moduleId, lecturerId)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Dozentenzuweisung fehlgeschlagen: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungAppointmentLecturer(appointmentId: UUID, overrideModule: Boolean, lecturerId: String?) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.updateVerwaltungAppointmentLecturer(appointmentId.toString(), overrideModule, lecturerId)
                _refreshTrigger.value++
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Dozentenzuweisung fehlgeschlagen: ${error.message}"))
            }
        }
    }

    fun updateVerwaltungAttendance(
        appointmentId: UUID,
        studentId: String,
        status: AttendanceStatus
    ) {
        _internalState.update {
            it.copy(
                attendanceEditorStatusOverrides = it.attendanceEditorStatusOverrides + (studentId to status)
            )
        }

        viewModelScope.launch {
            delay(SWITCH_ANIMATION_DELAY_MILLIS.milliseconds)
            _internalState.update {
                it.copy(pendingAttendanceStudentIds = it.pendingAttendanceStudentIds + studentId)
            }

            runCatching {
                backendRemoteDataSource.updateVerwaltungAttendance(
                    appointmentId = appointmentId.toString(),
                    studentId = studentId,
                    status = status.toDto()
                )
            }.onSuccess {
                _internalState.update {
                    it.copy(
                        pendingAttendanceStudentIds = it.pendingAttendanceStudentIds - studentId,
                        attendanceEditorDisplayedStatusOverrides =
                            it.attendanceEditorDisplayedStatusOverrides + (studentId to status)
                    )
                }
                _refreshTrigger.value++
            }.onFailure {
                _internalState.update {
                    it.copy(
                        pendingAttendanceStudentIds = it.pendingAttendanceStudentIds - studentId,
                        attendanceEditorStatusOverrides =
                            it.attendanceEditorStatusOverrides - studentId
                    )
                }
            }
        }
    }

    fun validateVerwaltungAttendances(appointmentId: UUID) {
        viewModelScope.launch {
            try {
                backendRemoteDataSource.validateVerwaltungAttendances(appointmentId.toString())
                _refreshTrigger.value++
                closeSheet()
            } catch (error: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("Anwesenheiten konnten nicht bestätigt werden: ${error.message}"))
            }
        }
    }

    fun closeSheet() {
        _internalState.update {
            it.copy(
                activeSheet = null,
                verwaltungAttendanceCourseId = null,
                attendanceEditorStatusOverrides = emptyMap(),
                attendanceEditorDisplayedStatusOverrides = emptyMap(),
                pendingAttendanceStudentIds = emptySet()
            )
        }
    }

    private data class HomeInternalState(
        val checkInState: CheckInState = CheckInState.Scanning(),
        val selectedDay: Day = Day.Today,
        val activeSheet: HomeSheet? = null,
        val attendanceEditorStatusOverrides: Map<String, AttendanceStatus> = emptyMap(),
        val attendanceEditorDisplayedStatusOverrides: Map<String, AttendanceStatus> = emptyMap(),
        val pendingAttendanceStudentIds: Set<String> = emptySet(),
        val verwaltungSearchQuery: String = "",
        val verwaltungAttendanceCourseId: String? = null,
        val selectedVerwaltungAppointmentId: UUID? = null,
        val verwaltungActivationCodes: Map<String, String> = emptyMap(),
        val refreshIsLoading: Boolean = false,
    )

    private data class HomeBackendData(
        val studentHomeData: BackendResponses.StudentHomeData? = null,
        val dozentHomeData: BackendResponses.DozentHomeData? = null,
        val verwaltungHomeData: BackendResponses.VerwaltungHomeData? = null
    )
}

private fun BackendDtos.Appointment.toAppointment(): Appointment =
    Appointment(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.nameUUIDFromBytes(id.toByteArray()) },
        title = title,
        room = room,
        startAt = Instant.ofEpochMilli(startAt.toEpochMilliseconds()),
        endAt = Instant.ofEpochMilli(endAt.toEpochMilliseconds()),
        course = course,
        protokollant = protokollant,
        moduleId = moduleId,
        ignored = ignored,
        lecturerId = lecturerId,
        lecturerOverride = lecturerOverride,
        lecturerEditable = lecturerEditable
    )

private fun BackendDtos.Attendance.toStudentAttendance(): StudentAttendance =
    StudentAttendance(
        studentId = student.studentId,
        displayName = student.displayName ?: student.studentId,
        status = status.toAttendanceStatus()
    )

private fun BackendDtos.Course.toVerwaltungCourse(): VerwaltungCourse =
    VerwaltungCourse(
        id = id,
        name = name
    )

private fun BackendDtos.Student.toVerwaltungUser(): VerwaltungUser =
    VerwaltungUser(
        studentId = studentId,
        displayName = displayName ?: studentId,
        activatedSince = activatedSinceEpochMillis?.let { Instant.ofEpochMilli(it) },
        generatedActivationCode = generatedActivationCode
    )

private fun BackendResponses.AttendanceDay.toVerwaltungAttendanceDay(): VerwaltungAttendanceDay =
    VerwaltungAttendanceDay(
        appointment = appointment.toAppointment(),
        attendances = attendances.map { it.toStudentAttendance() },
        attendancesValidated = attendancesValidated
    )

private fun BackendResponses.CourseDetail.toVerwaltungCourseDetail(
    allModules: List<BackendDtos.Module>,
    enrollments: Map<String, Set<String>>,
    generatedActivationCodes: Map<String, String>,
    lecturers: List<BackendDtos.Lecturer>
): VerwaltungCourseDetail =
    VerwaltungCourseDetail(
        course = course.toVerwaltungCourse(),
        users = students.map { student ->
            student.copy(
                generatedActivationCode =
                    generatedActivationCodes[student.studentId] ?: student.generatedActivationCode
            ).toVerwaltungUser()
        },
        appointments = appointments.map { it.toAppointment() },
        attendanceDays = attendanceDays.map { it.toVerwaltungAttendanceDay() },
        availableModules = allModules.map { VerwaltungModule(it.id, it.courseGroupId, it.name, it.ignored, it.lecturerId) },
        moduleEnrollments = enrollments,
        lecturers = lecturers
    )

private fun BackendDtos.Absence.toReportedAbsence(): ReportedAbsence =
    ReportedAbsence(
        id = id.ifBlank { "$fromEpochMillis-$untilEpochMillis" },
        fromAt = Instant.ofEpochMilli(fromEpochMillis),
        untilAt = Instant.ofEpochMilli(untilEpochMillis),
        status = status
    )

private fun ReportedAbsence.contains(instant: Instant): Boolean =
    !fromAt.isAfter(instant) && untilAt.isAfter(instant)

private fun ReportedAbsence.isFullDay(): Boolean {
    val zoneId = ZoneId.systemDefault()
    val fromDateTime = fromAt.atZone(zoneId).toLocalDateTime()
    val untilDateTime = untilAt.atZone(zoneId).toLocalDateTime()
    return fromDateTime.toLocalTime() == LocalTime.MIN &&
            untilDateTime.toLocalTime() >= LocalTime.of(23, 59)
}

private fun AttendanceStatus.toDto(): AttendanceStatusDto =
    when (this) {
        AttendanceStatus.Missing -> AttendanceStatusDto.MISSING
        AttendanceStatus.Present -> AttendanceStatusDto.PRESENT
        AttendanceStatus.Late -> AttendanceStatusDto.LATE
        AttendanceStatus.Partially -> AttendanceStatusDto.PARTIALLY
        AttendanceStatus.Absent -> AttendanceStatusDto.ABSENT
        AttendanceStatus.Excused -> AttendanceStatusDto.EXCUSED
        AttendanceStatus.ManualReview -> AttendanceStatusDto.MANUAL_REVIEW
    }

private fun AttendanceStatusDto.toAttendanceStatus(): AttendanceStatus =
    when (this) {
        AttendanceStatusDto.MISSING -> AttendanceStatus.Missing
        AttendanceStatusDto.PRESENT -> AttendanceStatus.Present
        AttendanceStatusDto.LATE -> AttendanceStatus.Late
        AttendanceStatusDto.PARTIALLY -> AttendanceStatus.Partially
        AttendanceStatusDto.ABSENT -> AttendanceStatus.Absent
        AttendanceStatusDto.EXCUSED -> AttendanceStatus.Excused
        AttendanceStatusDto.MANUAL_REVIEW -> AttendanceStatus.ManualReview
    }

private const val SWITCH_ANIMATION_DELAY_MILLIS = 250L
