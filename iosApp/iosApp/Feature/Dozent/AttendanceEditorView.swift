import SwiftUI

enum AttendanceSheetRoute: String, Identifiable {
    case overview
    case editor
    case confirmationEditor

    var id: String { rawValue }
}

struct AttendanceEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: HomeBackendViewModel
    let route: AttendanceSheetRoute
    
    var body: some View {
        NavigationStack {
            List {
                Section("Fehlend") {
                    ForEach(attendances(with: [.missing])) { attendance in
                        attendanceRow(attendance)
                    }
                }
                
                Section("Anwesend") {
                    ForEach(attendances(with: [.present])) { attendance in
                        attendanceRow(attendance)
                    }
                }

                Section("Verspätet") {
                    ForEach(attendances(with: [.late])) { attendance in
                        attendanceRow(attendance)
                    }
                }

                Section("Abwesend") {
                    ForEach(attendances(with: [.absent])) { attendance in
                        attendanceRow(attendance)
                    }
                }
            }
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Schließen", systemImage: "xmark") { dismiss() }
                }
                if route == .confirmationEditor {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Validieren", systemImage: "checkmark") {
                            viewModel.validateAttendances()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color(.baGreen))
                        .disabled(
                            viewModel.isLoading
                                || viewModel.attendancesValidated
                                || !viewModel.hasSelectedProtokollant
                                || !viewModel.pendingStudentIds.isEmpty
                        )
                    }
                }
            }
            .onChange(of: viewModel.attendancesValidated) { _, isValidated in
                if route == .confirmationEditor && isValidated {
                    dismiss()
                }
            }
        }
    }

    private var navigationTitle: String {
        switch route {
        case .overview:
            "Anwesenheiten"
        case .editor:
            "Anwesenheiten bearbeiten"
        case .confirmationEditor:
            "Anwesenheiten bestätigen"
        }
    }

    private func attendances(
        with statuses: Set<HomeAttendanceStatus>
    ) -> [HomeAttendance] {
        viewModel.attendances.filter { statuses.contains($0.status) }
    }

    private func attendanceRow(_ attendance: HomeAttendance) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(attendance.displayName)
            }
            Spacer()
            if route == .overview {
                AttendanceStatusIcon(status: attendance.status, lateMinutes: attendance.lateMinutes)
            } else if isLockedForCurrentRole(attendance) {
                AttendanceStatusIcon(status: .absent)
                    .accessibilityLabel("Abwesenheit kann nicht bearbeitet werden")
            } else if viewModel.pendingStudentIds.contains(attendance.id) {
                ProgressView()
            } else {
                Toggle(
                    attendance.status.rawValue,
                    isOn: Binding(
                        get: {
                            attendance.status == .present || attendance.status == .late
                        },
                        set: {
                            viewModel.setAttendance(
                                studentId: attendance.id,
                                isPresent: $0
                            )
                        }
                    )
                )
                .labelsHidden()
                .tint(Color(.baGreen))
                .background(Color(.baRed), in: Capsule())
            }
        }
    }

    private func isLockedForCurrentRole(_ attendance: HomeAttendance) -> Bool {
        viewModel.role == "DOZENT" && attendance.status == .absent
    }
}

private struct AttendanceOverviewPreview: View {
    @StateObject private var viewModel = HomeBackendViewModel(role: "DOZENT")

    var body: some View {
        AttendanceEditorView(viewModel: viewModel, route: .overview)
            .task {
                viewModel.load()
            }
    }
}

#Preview("Nur Icons") {
    AttendanceOverviewPreview()
}
