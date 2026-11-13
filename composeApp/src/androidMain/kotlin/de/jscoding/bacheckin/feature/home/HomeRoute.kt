package de.jscoding.bacheckin.feature.home

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.jscoding.bacheckin.core.security.BiometricGate
import de.jscoding.bacheckin.core.security.BiometricGateResult
import de.jscoding.bacheckin.core.ui.components.BCINoInternetView
import de.jscoding.bacheckin.core.ui.components.SessionStatus
import de.jscoding.bacheckin.feature.home.dozent.DozentHomeScreen
import de.jscoding.bacheckin.feature.home.student.StudentHomeScreen
import de.jscoding.bacheckin.feature.home.verwaltung.VerwaltungHomeScreen
import de.jscoding.bacheckin.shared.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remaining by viewModel.inactivityRemaining.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingActivationCodeRequest by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(viewModel, uiState.userRole) {
        if (uiState.userRole == UserRole.STUDENT || uiState.userRole == UserRole.VERWALTUNG) {
            viewModel.refreshBackendHomeData()
        }
        val refreshJob = if (uiState.userRole == UserRole.VERWALTUNG) {
            scope.launch {
                while (true) {
                    delay(15_000)
                    viewModel.refreshBackendHomeData()
                }
            }
        } else {
            null
        }
        onPauseOrDispose { refreshJob?.cancel() }
    }

    LifecycleResumeEffect(activity, uiState.checkInState, uiState.userRole) {
        if (activity != null && uiState.userRole == UserRole.STUDENT && uiState.checkInState is CheckInState.Scanning) {
            viewModel.startNfcCheckIn(activity)
        }
        onPauseOrDispose {
            viewModel.stopNfcCheckIn()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.markInteraction()
    }

    LaunchedEffect(remaining) {
        if (remaining <= 0L) {
            onSessionExpired()
        }
    }


    if (!uiState.isOnline) {
        BCINoInternetView()
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { evt ->
            when (evt) {
                is HomeViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(evt.message)
                }

                is HomeViewModel.UiEvent.ShowSpinner -> {
                    isRefreshing = evt.show
                }
            }
        }
    }

    LaunchedEffect(pendingActivationCodeRequest) {
        val studentId = pendingActivationCodeRequest
        if (studentId != null) {
            if (activity != null) {
                scope.launch {
                    when (BiometricGate(activity).authenticate(
                        useBiometrics = true,
                        title = "Freischaltcode generieren",
                        subtitle = "Bestätigung erforderlich"
                    )) {
                        BiometricGateResult.Success,
                        BiometricGateResult.NotAvailable -> {
                            viewModel.createVerwaltungUserActivationCode(studentId)
                        }
                        BiometricGateResult.Cancelled -> {
                            snackbarHostState.showSnackbar("Freischaltcode-Generierung abgebrochen")
                        }
                        is BiometricGateResult.Error -> {
                            viewModel.createVerwaltungUserActivationCode(studentId)
                        }
                    }
                    pendingActivationCodeRequest = null
                }
            } else {
                viewModel.createVerwaltungUserActivationCode(studentId)
                pendingActivationCodeRequest = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                            viewModel.markInteraction()
                        }
                    }
                }
            }
    ) {
        val sessionActions: @Composable RowScope.() -> Unit = {
            SessionStatus(
                inactivityRemainingMs = remaining,
                onManualRefresh = { viewModel.manualRefresh() }
            )
        }

        when (uiState.userRole) {
            UserRole.STUDENT -> {
                StudentHomeScreen(
                    uiState = uiState,
                    onOpenSettings = onOpenSettings,
                    onSelectDay = viewModel::selectDay,
                    onRetryCheckIn = viewModel::retryNfcCheckIn,
                    onOpenAbwesenheitSheet = viewModel::openAbwesenheitSheet,
                    onReportAbsence = viewModel::reportAbsence,
                    onCloseSheet = viewModel::closeSheet,
                    actionsBesideSettings = sessionActions
                )
            }

            UserRole.DOZENT -> {
                DozentHomeScreen(
                    uiState = uiState,
                    onOpenSettings = onOpenSettings,
                    onSelectDay = viewModel::selectDay,
                    onCloseSheet = viewModel::closeSheet,
                    onAcceptAppointment = viewModel::acceptAppointment,
                    onOpenAppointment = viewModel::openAppointment,
                    onSelectProtokollant = viewModel::selectAppointmentProtokollant,
                    onOpenAttendanceEdit = viewModel::openAttendancesEditSheet,
                    onUpdateStudentAttendance = viewModel::updateStudentAttendance,
                    onValidateAttendances = viewModel::validateAppointmentAttendances,
                    actionsBesideSettings = sessionActions
                )
            }

            UserRole.VERWALTUNG -> {
                VerwaltungHomeScreen(
                    uiState = uiState,
                    onOpenSettings = onOpenSettings,
                    onSearchQueryChange = viewModel::updateVerwaltungSearchQuery,
                    onOpenAttendances = viewModel::openVerwaltungAttendances,
                    onOpenAttendanceEdit = viewModel::openVerwaltungAttendanceEditSheet,
                    onUpdateAttendance = viewModel::updateVerwaltungAttendance,
                    onValidateAttendances = viewModel::validateVerwaltungAttendances,
                    onCreateActivationCode = { studentId -> pendingActivationCodeRequest = studentId },
                    onRevokeDevice = viewModel::revokeVerwaltungUserDevice,
                    onSetStudentModule = viewModel::setStudentModule,
                    onSetAppointmentIgnored = viewModel::updateVerwaltungAppointmentIgnored,
                    onSetModuleIgnored = viewModel::updateVerwaltungModuleIgnored,
                    onSetModuleLecturer = viewModel::updateVerwaltungModuleLecturer,
                    onSetAppointmentLecturer = viewModel::updateVerwaltungAppointmentLecturer,
                    onReviewAbsence = viewModel::reviewAbsence,
                    onLoadBackendData = viewModel::refreshBackendHomeData,
                    onRefreshCalendar = {
                        Toast.makeText(context, "Demo geladen", Toast.LENGTH_SHORT).show()
                    },
                    onCloseSheet = viewModel::closeSheet,
                    actionsBesideSettings = sessionActions
                )
            }

            UserRole.NONE -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nicht autorisiert")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
