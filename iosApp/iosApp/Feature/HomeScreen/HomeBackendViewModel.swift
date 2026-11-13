import Foundation
import Shared

struct HomeAppointment: Identifiable, Hashable {
    let id: String
    let title: String
    let room: String
    let course: String
    let protokollant: String
    let startAt: Date
    let endAt: Date
    let moduleID: String?
    let ignored: Bool
    let lecturerID: String?
    let lecturerOverride: Bool
    let lecturerEditable: Bool

    init(
        id: String,
        title: String,
        room: String,
        course: String,
        protokollant: String,
        startAt: Date,
        endAt: Date,
        moduleID: String? = nil,
        ignored: Bool = false,
        lecturerID: String? = nil,
        lecturerOverride: Bool = false,
        lecturerEditable: Bool = false
    ) {
        self.id = id
        self.title = title
        self.room = room
        self.course = course
        self.protokollant = protokollant
        self.startAt = startAt
        self.endAt = endAt
        self.moduleID = moduleID
        self.ignored = ignored
        self.lecturerID = lecturerID
        self.lecturerOverride = lecturerOverride
        self.lecturerEditable = lecturerEditable
    }
}

struct HomeModule: Identifiable, Hashable {
    let id: String
    let courseGroupID: String
    let name: String
    let ignored: Bool
    let lecturerID: String?
}

struct HomeLecturer: Identifiable, Hashable {
    let id: String
    let displayName: String
    let active: Bool
}

struct HomeAbsence: Identifiable, Hashable {
    let id: String
    let fromAt: Date
    let untilAt: Date
    let status: String
}

struct HomeCourse: Identifiable, Hashable {
    let id: String
    let name: String
    var students: [HomeStudent]
    var modules: [HomeModule]
    var appointments: [HomeAppointment]
    var attendanceDays: [HomeAttendanceDay]

    init(
        id: String,
        name: String,
        students: [HomeStudent],
        modules: [HomeModule] = [],
        appointments: [HomeAppointment] = [],
        attendanceDays: [HomeAttendanceDay] = []
    ) {
        self.id = id
        self.name = name
        self.students = students
        self.modules = modules
        self.appointments = appointments
        self.attendanceDays = attendanceDays
    }
}

struct HomeAttendanceDay: Identifiable, Hashable {
    let appointment: HomeAppointment
    let attendances: [HomeAttendance]

    var id: String { appointment.id }
    var presentCount: Int {
        attendances.filter { $0.status == .present || $0.status == .late }.count
    }
}

struct HomeStudent: Identifiable, Hashable {
    let id: String
    let displayName: String
    var activationCode: String? = nil
    var activatedSince: Date? = nil
}

enum CheckInStatus {
    case notCheckedIn
    case checkedIn(until: Date)
    case absent(until: Date)
    case waiting(message: String)
    case nfcUnavailable
    case unavailable
    case error(message: String)
}

enum HomeAttendanceStatus: String {
    case missing = "Fehlend"
    case present = "Anwesend"
    case late = "Verspätet"
    case absent = "Abwesend"

    var isPresent: Bool { self == .present || self == .late }

    var systemImage: String {
        switch self {
        case .missing: "questionmark.circle"
        case .present: "checkmark.circle.fill"
        case .late: "clock.fill"
        case .absent: "xmark.circle.fill"
        }
    }
}

struct HomeAttendance: Identifiable, Hashable {
    let id: String
    let displayName: String
    var status: HomeAttendanceStatus
    var lateMinutes: Int? = nil
}

struct HomeAttendanceChange {
    let appointmentId: String
    let studentId: String
    let status: HomeAttendanceStatus
}

enum SecretaryRoute: Hashable {
    case course(String)
    case student(String)
}

@MainActor
final class HomeBackendViewModel: ObservableObject {
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var studentId: String?
    @Published private(set) var appointments: [HomeAppointment] = []
    @Published private(set) var courses: [HomeCourse] = []
    @Published private(set) var lecturers: [HomeLecturer] = []
    @Published private(set) var absences: [HomeAbsence] = []
    @Published private(set) var currentCheckInUntil: Date?
    @Published private(set) var isNFCCheckInAvailable = false
    @Published private(set) var checkInAvailabilityState = "UNAVAILABLE"
    @Published private(set) var checkInOpensAt: Date?
    @Published private(set) var activeAppointmentId: String?
    @Published private(set) var attendances: [HomeAttendance] = []
    @Published private(set) var attendancesValidated = false
    @Published private(set) var pendingStudentIds: Set<String> = []
    @Published private(set) var isSavingSecretaryAttendances = false

    let role: String
    private let backend: any BackendRemoteDataSource
    private let backendSession: BackendSession

    init(
        role: String,
        backendSession: BackendSession = .shared
    ) {
        self.role = role.uppercased()
        self.backendSession = backendSession
        self.backend = backendSession.backend
    }

    var activeAppointment: HomeAppointment? {
        appointments.first { $0.id == activeAppointmentId }
    }

    var hasSelectedProtokollant: Bool {
        guard let activeAppointment else { return false }
        return attendances.contains {
            $0.displayName == activeAppointment.protokollant
        }
    }

    var checkInStatus: CheckInStatus {
        var status: CheckInStatus = .notCheckedIn

        if let currentCheckInUntil {
            status = .checkedIn(until: currentCheckInUntil)
        }

        if let activeAbsence = absences.first(where: {
            $0.status != "REJECTED" && $0.fromAt <= Date() && $0.untilAt > Date()
        }) {
            status = .absent(until: activeAbsence.untilAt)
        } else if currentCheckInUntil == nil {
            switch checkInAvailabilityState {
            case "AVAILABLE":
                status = .notCheckedIn
            case "TOO_EARLY":
                let message = checkInOpensAt.map {
                    "Check-in möglich ab \($0.formatted(date: .omitted, time: .shortened)) Uhr"
                } ?? "Der nächste Check-in ist noch nicht freigeschaltet."
                status = .waiting(message: message)
            case "OVERLAPPING_LECTURES":
                status = .waiting(message: "Mehrere Veranstaltungen überschneiden sich.")
            case "NO_UPCOMING_LECTURE":
                status = .waiting(message: "Keine kommende Veranstaltung gefunden.")
            default:
                status = .unavailable
            }
        }

        if let errorMessage {
            status = .error(message: errorMessage)
        }

        return status
    }

    func clearError() {
        errorMessage = nil
    }

    func load() {
        isLoading = true
        errorMessage = nil

        if role == "VERWALTUNG" {
            loadVerwaltung()
        } else if role == "DOZENT" {
            loadDozent()
        } else {
            loadStudent()
        }
    }

    func reportAbsence(from: Date, until: Date) {
        guard from < until else {
            errorMessage = "Das Ende der Abwesenheit muss nach dem Beginn liegen."
            return
        }
        isLoading = true
        withAuthenticatedBackend { [weak self] backend in
            backend.reportAbsence(
                fromEpochMillis: Int64(from.timeIntervalSince1970 * 1_000),
                untilEpochMillis: Int64(until.timeIntervalSince1970 * 1_000)
            ) { error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error) } else { self.loadStudent() }
                }
            }
        }
    }

    func checkInWithNFC(
        cardPublicId: String,
        encryptedPiccDataHex: String,
        sdmMacHex: String
    ) {
        guard isNFCCheckInAvailable else { return }
        isLoading = true
        errorMessage = nil
        withAuthenticatedBackend { [weak self] backend in
            backend.verifyNfcCheckIn(
                request: BackendRequests.NfcCheckInRequest(
                    cardPublicId: cardPublicId,
                    encryptedPiccDataHex: encryptedPiccDataHex,
                    sdmMacHex: sdmMacHex
                )
            ) { result, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error {
                        self.finish(error: error)
                        return
                    }
                    guard let result else {
                        self.finishMissingResponse()
                        return
                    }
                    guard result.result == "CHECKED_IN",
                          let validUntil = result.validUntilEpochMillis else {
                        self.errorMessage = result.message ?? "Der NFC-Check-in ist fehlgeschlagen."
                        self.isLoading = false
                        return
                    }
                    self.currentCheckInUntil = Date(
                        timeIntervalSince1970: Double(validUntil.int64Value) / 1_000
                    )
                    self.isNFCCheckInAvailable = false
                    self.isLoading = false
                }
            }
        }
    }

    func reportNFCError(_ message: String) {
        errorMessage = message
    }

    func openAppointment(id: String) {
        isLoading = true
        withAuthenticatedBackend { [weak self] backend in
            backend.openAppointment(appointmentId: id) { overview, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let overview else { self.finishMissingResponse(); return }
                    self.apply(overview: overview)
                    self.isLoading = false
                }
            }
        }
    }

    func setAttendance(studentId: String, isPresent: Bool) {
        guard let appointmentId = activeAppointmentId else { return }
        guard !(role == "DOZENT" && attendances.first {
            $0.id == studentId
        }?.status == .absent) else { return }
        pendingStudentIds.insert(studentId)
        let status = isPresent ? AttendanceStatusDto.present : AttendanceStatusDto.missing
        withAuthenticatedBackend { [weak self] backend in
            backend.updateStudentAttendance(
                appointmentId: appointmentId,
                studentId: studentId,
                status: status
            ) { day, error in
                Task { @MainActor in
                    guard let self else { return }
                    self.pendingStudentIds.remove(studentId)
                    if let error { self.finish(error: error); return }
                    guard let day else { self.finishMissingResponse(); return }
                    self.apply(attendanceDay: day)
                }
            }
        }
    }

    func validateAttendances() {
        guard hasSelectedProtokollant,
              let appointmentId = activeAppointmentId else { return }
        isLoading = true
        withAuthenticatedBackend { [weak self] backend in
            backend.validateAppointmentAttendances(appointmentId: appointmentId) { day, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let day else { self.finishMissingResponse(); return }
                    self.apply(attendanceDay: day)
                    self.isLoading = false
                }
            }
        }
    }

    func selectProtokollant(studentId: String) {
        guard let appointmentId = activeAppointmentId else { return }
        isLoading = true
        withAuthenticatedBackend { [weak self] backend in
            backend.selectAppointmentProtokollant(
                appointmentId: appointmentId,
                studentId: studentId
            ) { overview, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let overview else { self.finishMissingResponse(); return }
                    self.apply(overview: overview)
                    self.isLoading = false
                }
            }
        }
    }

    func saveSecretaryAttendances(_ changes: [HomeAttendanceChange]) async throws {
        guard !isSavingSecretaryAttendances else {
            throw NSError(domain: "Attendance", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "Die Anwesenheiten werden bereits gespeichert."
            ])
        }
        isSavingSecretaryAttendances = true
        defer { isSavingSecretaryAttendances = false }
        try await backendSession.prepareAuthenticatedBackend()

        // Apply each successful response immediately so a retry only sends unsaved changes.
        for change in changes {
            let status: AttendanceStatusDto
            switch change.status {
            case .missing: status = .missing
            case .present: status = .present
            case .late: status = .late
            case .absent: status = .absent
            }
            let response: BackendResponses.AttendanceDay = try await withCheckedThrowingContinuation { continuation in
                backend.updateVerwaltungAttendance(
                    appointmentId: change.appointmentId,
                    studentId: change.studentId,
                    status: status
                ) { day, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if let day {
                        continuation.resume(returning: day)
                    } else {
                        continuation.resume(throwing: NSError(domain: "Attendance", code: 2, userInfo: [
                            NSLocalizedDescriptionKey: "Das Backend hat keine Daten zurückgegeben."
                        ]))
                    }
                }
            }
            let updatedDay = HomeAttendanceDay(
                appointment: Self.mapAppointment(response.appointment),
                attendances: response.attendances.map(Self.mapAttendance)
            )
            for courseIndex in courses.indices {
                for dayIndex in courses[courseIndex].attendanceDays.indices
                    where courses[courseIndex].attendanceDays[dayIndex].id == updatedDay.id {
                    courses[courseIndex].attendanceDays[dayIndex] = updatedDay
                }
            }
            if activeAppointmentId == updatedDay.id {
                apply(attendanceDay: response)
            }
        }
    }

    func createActivationCode(studentId: String) {
        guard !pendingStudentIds.contains(studentId) else { return }
        pendingStudentIds.insert(studentId)
        errorMessage = nil
        withAuthenticatedBackend { [weak self] backend in
            backend.createVerwaltungUserActivationCode(studentId: studentId) { student, error in
                Task { @MainActor in
                    self?.finishStudentUpdate(studentId: studentId, student: student, error: error)
                }
            }
        }
    }

    func revokeStudentDevice(studentId: String) {
        guard !pendingStudentIds.contains(studentId) else { return }
        pendingStudentIds.insert(studentId)
        errorMessage = nil
        withAuthenticatedBackend { [weak self] backend in
            backend.revokeVerwaltungUserDevice(studentId: studentId) { student, error in
                Task { @MainActor in
                    self?.finishStudentUpdate(studentId: studentId, student: student, error: error)
                }
            }
        }
    }

    func setModuleLecturer(moduleId: String, lecturerId: String?) {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        withAuthenticatedBackend { [weak self] backend in
            backend.updateVerwaltungModuleLecturer(
                moduleId: moduleId,
                lecturerId: lecturerId
            ) { error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error {
                        self.finish(error: error)
                    } else {
                        self.loadVerwaltung()
                    }
                }
            }
        }
    }

    func setAppointmentLecturer(
        appointmentId: String,
        overrideModule: Bool,
        lecturerId: String?
    ) {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        withAuthenticatedBackend { [weak self] backend in
            backend.updateVerwaltungAppointmentLecturer(
                appointmentId: appointmentId,
                overrideModule: overrideModule,
                lecturerId: lecturerId
            ) { error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error {
                        self.finish(error: error)
                    } else {
                        self.loadVerwaltung()
                    }
                }
            }
        }
    }

    private func finishStudentUpdate(studentId: String, student: BackendDtos.Student?, error: Error?) {
        pendingStudentIds.remove(studentId)
        if let error { finish(error: error); return }
        guard let student else { finishMissingResponse(); return }
        let updatedStudent = Self.mapStudent(student)
        for courseIndex in courses.indices {
            for studentIndex in courses[courseIndex].students.indices
                where courses[courseIndex].students[studentIndex].id == studentId {
                courses[courseIndex].students[studentIndex] = updatedStudent
            }
        }
    }

    private func loadVerwaltung() {
        withAuthenticatedBackend { [weak self] backend in
            backend.getVerwaltungHomeData { data, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let data else { self.finishMissingResponse(); return }
                    self.lecturers = data.lecturers.map(Self.mapLecturer)
                        .sorted { $0.displayName.localizedStandardCompare($1.displayName) == .orderedAscending }
                    self.courses = data.courseDetails.map { detail in
                        HomeCourse(
                            id: detail.course.id,
                            name: detail.course.name,
                            students: detail.students.map(Self.mapStudent)
                                .sorted { $0.displayName.localizedStandardCompare($1.displayName) == .orderedAscending },
                            modules: detail.modules.map(Self.mapModule)
                                .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending },
                            appointments: detail.appointments.map(Self.mapAppointment),
                            attendanceDays: detail.attendanceDays.map { day in
                                HomeAttendanceDay(
                                    appointment: Self.mapAppointment(day.appointment),
                                    attendances: day.attendances.map(Self.mapAttendance)
                                )
                            }
                        )
                    }.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
                    self.isLoading = false
                }
            }
        }
    }

    private func loadStudent() {
        withAuthenticatedBackend { [weak self] backend in
            backend.getStudentHomeData { data, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let data else { self.finishMissingResponse(); return }
                    self.studentId = data.student.studentId
                    self.appointments = data.appointments.map(Self.mapAppointment)
                    self.absences = data.absences.map(Self.mapAbsence).sorted { $0.fromAt > $1.fromAt }
                    self.checkInAvailabilityState = data.checkInAvailability.state
                    self.checkInOpensAt = data.checkInAvailability.opensAt.map {
                        Date(timeIntervalSince1970: Double($0.toEpochMilliseconds()) / 1_000)
                    }
                    self.isNFCCheckInAvailable = data.checkInAvailability.state == "AVAILABLE"
                    if let attendanceStatus = data.checkInAvailability.attendanceStatus,
                       ["PRESENT", "LATE", "PARTIALLY"].contains(attendanceStatus) {
                        self.currentCheckInUntil = data.checkInAvailability.validUntil.map {
                            Date(timeIntervalSince1970: Double($0.toEpochMilliseconds()) / 1_000)
                        }
                    } else {
                        self.currentCheckInUntil = nil
                    }
                    self.isLoading = false
                }
            }
        }
    }

    private func loadDozent() {
        withAuthenticatedBackend { [weak self] backend in
            backend.getDozentHomeData { data, error in
                Task { @MainActor in
                    guard let self else { return }
                    if let error { self.finish(error: error); return }
                    guard let data else { self.finishMissingResponse(); return }
                    self.apply(overview: data.appointmentOverview)
                    self.isLoading = false
                }
            }
        }
    }

    private func withAuthenticatedBackend(
        _ action: @escaping (any BackendRemoteDataSource) -> Void
    ) {
        backendSession.prepareAuthenticatedBackend { [weak self] error in
            Task { @MainActor in
                guard let self else { return }
                if let error {
                    self.pendingStudentIds.removeAll()
                    self.finish(error: error)
                    return
                }
                action(self.backend)
            }
        }
    }

    private func apply(overview: BackendResponses.AppointmentOverview) {
        appointments = overview.appointments.map(Self.mapAppointment)
        activeAppointmentId = overview.activeAppointmentId
        if let day = overview.activeAttendanceDay {
            apply(attendanceDay: day)
        } else {
            attendances = []
            attendancesValidated = false
        }
    }

    private func apply(attendanceDay: BackendResponses.AttendanceDay) {
        activeAppointmentId = attendanceDay.appointment.id
        attendances = attendanceDay.attendances.map(Self.mapAttendance)
        attendancesValidated = attendanceDay.attendancesValidated
        errorMessage = nil
    }

    private func finish(error: Error) {
        errorMessage = error.localizedDescription
        if role == "STUDENT" {
            isNFCCheckInAvailable = false
        }
        isLoading = false
    }

    private func finishMissingResponse() {
        errorMessage = "Das Backend hat keine Daten zurückgegeben."
        isLoading = false
    }

    private static func mapStudent(_ value: BackendDtos.Student) -> HomeStudent {
        HomeStudent(
            id: value.studentId,
            displayName: value.displayName ?? value.studentId,
            activationCode: value.generatedActivationCode,
            activatedSince: value.activatedSinceEpochMillis.map {
                Date(timeIntervalSince1970: Double($0.int64Value) / 1_000)
            }
        )
    }

    private static func mapAttendance(_ value: BackendDtos.Attendance) -> HomeAttendance {
        HomeAttendance(
            id: value.student.studentId,
            displayName: value.student.displayName ?? value.student.studentId,
            status: Self.mapStatus(value.status),
            lateMinutes: value.lateMinutes.map { Int($0.int32Value) }
        )
    }

    private static func mapAppointment(_ value: BackendDtos.Appointment) -> HomeAppointment {
        HomeAppointment(
            id: value.id,
            title: value.title,
            room: value.room,
            course: value.course,
            protokollant: value.protokollant.isEmpty ? "Noch nicht ausgewählt" : value.protokollant,
            startAt: Date(timeIntervalSince1970: Double(value.startAt.toEpochMilliseconds()) / 1_000),
            endAt: Date(timeIntervalSince1970: Double(value.endAt.toEpochMilliseconds()) / 1_000),
            moduleID: value.moduleId,
            ignored: value.ignored,
            lecturerID: value.lecturerId,
            lecturerOverride: value.lecturerOverride,
            lecturerEditable: value.lecturerEditable
        )
    }

    private static func mapModule(_ value: BackendDtos.Module) -> HomeModule {
        HomeModule(
            id: value.id,
            courseGroupID: value.courseGroupId,
            name: value.name,
            ignored: value.ignored,
            lecturerID: value.lecturerId
        )
    }

    private static func mapLecturer(_ value: BackendDtos.Lecturer) -> HomeLecturer {
        HomeLecturer(id: value.id, displayName: value.displayName, active: value.active)
    }

    private static func mapAbsence(_ value: BackendDtos.Absence) -> HomeAbsence {
        HomeAbsence(
            id: value.id.isEmpty ? "\(value.fromEpochMillis)-\(value.untilEpochMillis)" : value.id,
            fromAt: Date(timeIntervalSince1970: Double(value.fromEpochMillis) / 1_000),
            untilAt: Date(timeIntervalSince1970: Double(value.untilEpochMillis) / 1_000),
            status: value.status
        )
    }

    private static func mapStatus(_ value: AttendanceStatusDto) -> HomeAttendanceStatus {
        switch value.name {
        case "PRESENT": .present
        case "LATE": .late
        case "ABSENT": .absent
        default: .missing
        }
    }
}
