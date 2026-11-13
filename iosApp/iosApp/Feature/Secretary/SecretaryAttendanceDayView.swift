import SwiftUI

enum SecretaryAttendanceScope {
    case courseDay(String)
    case student(String)
    case studentDay(studentId: String, appointmentId: String)
}

struct SecretaryAttendanceDayView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: HomeBackendViewModel
    let scope: SecretaryAttendanceScope
    var startEditing = false
    @State private var isEditing = false
    @State private var drafts: [Entry.ID: HomeAttendanceStatus] = [:]
    @State private var saveError: String?

    private struct Entry: Identifiable {
        struct ID: Hashable {
            let appointmentId: String
            let studentId: String
        }

        let day: HomeAttendanceDay
        let attendance: HomeAttendance
        var id: ID { ID(appointmentId: day.id, studentId: attendance.id) }
    }

    private var isCourseDay: Bool {
        if case .courseDay = scope { return true }
        return false
    }

    private var entries: [Entry] {
        var seenIds = Set<Entry.ID>()
        return viewModel.courses.flatMap(\.attendanceDays).flatMap { day in
            day.attendances.compactMap { attendance -> Entry? in
                let included: Bool
                switch scope {
                case .courseDay(let appointmentId):
                    included = day.id == appointmentId
                case .student(let studentId):
                    included = attendance.id == studentId
                case .studentDay(let studentId, let appointmentId):
                    included = attendance.id == studentId && day.id == appointmentId
                }
                let entry = Entry(day: day, attendance: attendance)
                return included && seenIds.insert(entry.id).inserted ? entry : nil
            }
        }.sorted {
            if isCourseDay {
                return $0.attendance.displayName.localizedStandardCompare($1.attendance.displayName) == .orderedAscending
            }
            return $0.day.appointment.startAt > $1.day.appointment.startAt
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    if entries.isEmpty {
                        ContentUnavailableView("Keine Anwesenheiten vorhanden", systemImage: "person.2")
                    } else if isCourseDay {
                        attendanceSection("Fehlend", entries: entries.filter { $0.attendance.status == .missing })
                        attendanceSection("Anwesend", entries: entries.filter { $0.attendance.status.isPresent })
                        attendanceSection("Abwesend", entries: entries.filter { $0.attendance.status == .absent })
                    } else {
                        attendanceSection("Tage", entries: entries)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
            .background(Color(.systemBackground))
            .navigationTitle(isEditing ? "Anwesenheiten bearbeiten" : "Anwesenheiten")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(isEditing ? "Änderungen verwerfen und schließen" : "Schließen", systemImage: "xmark") {
                        dismiss()
                    }
                    .labelStyle(.iconOnly)
                    .tint(.secondary)
                    .disabled(viewModel.isSavingSecretaryAttendances)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if viewModel.isSavingSecretaryAttendances {
                        ProgressView()
                    } else {
                        Button(isEditing ? "Änderungen speichern" : "Bearbeiten", systemImage: isEditing ? "checkmark" : "pencil") {
                            if isEditing {
                                save()
                            } else {
                                beginEditing()
                            }
                        }
                        .labelStyle(.iconOnly)
                        .buttonStyle(.borderedProminent)
                        .tint(Color(isEditing ? .baGreen : .baPrimary))
                        .disabled(entries.isEmpty)
                    }
                }
            }
            .onAppear {
                if startEditing && !isEditing { beginEditing() }
            }
            .alert("Änderungen konnten nicht vollständig gespeichert werden", isPresented: Binding(
                get: { saveError != nil },
                set: { if !$0 { saveError = nil } }
            )) {
                Button("Erneut versuchen") { save() }
                Button("Weiter bearbeiten", role: .cancel) {}
            } message: {
                Text(saveError ?? "")
            }
        }
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(isEditing || viewModel.isSavingSecretaryAttendances)
    }

    private func attendanceSection(_ title: String, entries: [Entry]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 12)

            if entries.isEmpty {
                Text("Keine Einträge")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 12)
            } else {
                SecretaryListCard(items: entries) { entry in
                    attendanceRow(entry)
                }
            }
        }
    }

    private func attendanceRow(_ entry: Entry) -> some View {
        HStack(spacing: 12) {
            if isCourseDay {
                Text(entry.attendance.displayName)
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.day.appointment.startAt, format: .dateTime.day().month().year().locale(Locale(identifier: "de_DE")))
                    Text(entry.day.appointment.title)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
            if isEditing {
                AttendanceEditControl(status: Binding(
                    get: { drafts[entry.id] ?? entry.attendance.status },
                    set: { drafts[entry.id] = $0 }
                ))
                .disabled(viewModel.isSavingSecretaryAttendances)
            } else {
                AttendanceStatusIcon(
                    status: entry.attendance.status,
                    lateMinutes: isCourseDay ? nil : entry.attendance.lateMinutes
                )
            }
        }
        .padding(.vertical, isEditing ? 4 : 14)
        .frame(minHeight: 52)
    }

    private func beginEditing() {
        drafts = Dictionary(uniqueKeysWithValues: entries.map { ($0.id, $0.attendance.status) })
        isEditing = true
    }

    private func save() {
        let changes = entries.compactMap { entry -> HomeAttendanceChange? in
            guard let status = drafts[entry.id], status != entry.attendance.status else { return nil }
            return HomeAttendanceChange(appointmentId: entry.day.id, studentId: entry.attendance.id, status: status)
        }
        Task { @MainActor in
            do {
                try await viewModel.saveSecretaryAttendances(changes)
                drafts = [:]
                isEditing = false
                saveError = nil
            } catch {
                saveError = error.localizedDescription + " Bereits gespeicherte Änderungen bleiben erhalten."
            }
        }
    }
}

private struct SecretaryAttendanceDayPreview: View {
    @StateObject private var viewModel = HomeBackendViewModel(role: "VERWALTUNG")

    var body: some View {
        SecretaryAttendanceDayView(
            viewModel: viewModel,
            scope: .courseDay(SecretaryPreviewData.appointment.id)
        )
        .task { viewModel.load() }
    }
}

#Preview("Anwesenheiten eines Kurstages") {
    SecretaryAttendanceDayPreview()
}
