import SwiftUI

struct SecretaryStudentDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: HomeBackendViewModel
    let initialStudent: HomeStudent
    @State private var selectedTab: StudentTab = .management
    @State private var selectedAttendanceDay: HomeAttendanceDay?
    @State private var showRevokeConfirmation = false

    private var student: HomeStudent {
        viewModel.courses.lazy.flatMap(\.students).first { $0.id == initialStudent.id } ?? initialStudent
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                tabPicker
                    .accessibilityLabel("Nutzeransicht")

                switch selectedTab {
                case .management:
                    managementSection
                case .attendance:
                    SecretaryAttendanceDaysCard(
                        days: viewModel.courses.flatMap(\.attendanceDays),
                        studentId: student.id
                    ) {
                        selectedAttendanceDay = $0
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
        .background(Color(.systemBackground))
        .navigationTitle(student.displayName)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden()
        .toolbar(.visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Zurück", systemImage: "chevron.left") { dismiss() }
                    .labelStyle(.iconOnly)
                    .tint(.primary)
            }
        }
        .sheet(item: $selectedAttendanceDay) { day in
            SecretaryAttendanceDayView(
                viewModel: viewModel,
                scope: .studentDay(studentId: student.id, appointmentId: day.id),
                startEditing: true
            )
        }
        .confirmationDialog("Gerätezugang sperren?", isPresented: $showRevokeConfirmation, titleVisibility: .visible) {
            Button("Gerätezugang sperren", role: .destructive) {
                viewModel.revokeStudentDevice(studentId: student.id)
            }
            Button("Abbrechen", role: .cancel) {}
        } message: {
            Text("Das bisherige Gerät von \(student.displayName) verliert seinen Zugang. Der Nutzeraccount bleibt bestehen.")
        }

    }

    private var tabPicker: some View {
        HStack(spacing: 0) {
            ForEach(StudentTab.allCases, id: \.self) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    Text(tab.rawValue)
                        .font(.subheadline.weight(selectedTab == tab ? .semibold : .regular))
                        .foregroundStyle(selectedTab == tab ? .white : .primary)
                        .frame(maxWidth: .infinity, minHeight: 30)
                        .padding(.horizontal, 4)
                        .background {
                            if selectedTab == tab {
                                Capsule().fill(Color(.baPrimary))
                            }
                        }
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selectedTab == tab ? [.isSelected] : [])
            }
        }
        .padding(2)
        .background(Color(.systemGray6), in: Capsule())
    }

    private var managementSection: some View {
        let isUpdating = viewModel.pendingStudentIds.contains(student.id)

        return VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Aktueller Gerätecode")
                    .font(.headline)
                Text(student.activationCode ?? "Noch kein Code erstellt")
                    .foregroundStyle(student.activationCode == nil ? .secondary : .primary)
                    .textSelection(.enabled)

                Button {
                    viewModel.createActivationCode(studentId: student.id)
                } label: {
                    HStack(spacing: 6) {
                        if isUpdating {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "key.horizontal")
                        }
                        Text("Neuen Code")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.white)
                    .baFullWidthActionLabel()
                    .background(Color(.baPrimary), in: Capsule())
                }
                .buttonStyle(.plain)
                .disabled(isUpdating || viewModel.isLoading)
            }
            .padding(14)
            .cardBackground(cornerRadius: 28)
            .shadow(color: .black.opacity(0.08), radius: 16, y: 10)

            Button {
                showRevokeConfirmation = true
            } label: {
                Label(student.activatedSince == nil ? "Kein aktiver Gerätezugang" : "Gerätezugang sperren", systemImage: "lock")
                    .font(.subheadline)
                    .foregroundStyle(.primary)
                    .baFullWidthActionLabel()
                    .overlay { Capsule().stroke(Color(.baRed), lineWidth: 1) }
            }
            .buttonStyle(.plain)
            .disabled(isUpdating || viewModel.isLoading || student.activatedSince == nil)
        }
    }

    private enum StudentTab: String, CaseIterable {
        case management = "Nutzerverwaltung"
        case attendance = "Anwesenheiten"
    }
}

private struct SecretaryStudentDetailPreview: View {
    @StateObject private var viewModel = HomeBackendViewModel(role: "VERWALTUNG")

    var body: some View {
        NavigationStack {
            SecretaryStudentDetailView(
                viewModel: viewModel,
                initialStudent: SecretaryPreviewData.students[1]
            )
        }
        .task { viewModel.load() }
    }
}

#Preview("Nutzerdetails") {
    SecretaryStudentDetailPreview()
}
