import SwiftUI

struct StudentHomeView: View {
    @StateObject private var viewModel: HomeBackendViewModel
    @StateObject private var nfcController = StudentNFCCheckInController()
    @State private var showAbsenceSheet = false
    var onLogout: () -> Void

    init(role: String = "STUDENT", onLogout: @escaping () -> Void = {}) {
        _viewModel = StateObject(wrappedValue: HomeBackendViewModel(role: role))
        self.onLogout = onLogout
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    HomeHeader(
                        subtitle: viewModel.studentId.map { "Matrikelnummer: \($0)" },
                        onLogout: onLogout
                    )

                    CheckIn(
                        status: displayedCheckInStatus,
                        onScan: viewModel.isNFCCheckInAvailable && nfcController.isNFCAvailable
                            ? startNFCScanning
                            : nil
                    )

                    Button {
                        showAbsenceSheet = true
                    } label: {
                        Label("Abwesenheit melden", systemImage: "xmark.circle")
                            .foregroundStyle(.baPrimary)
                            .baFullWidthActionLabel()
                            .bold()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color("BaAbwesen"))
                    .padding(
                        EdgeInsets(
                            top: 8,
                            leading: 20,
                            bottom: 8,
                            trailing: 20
                        )
                    )
                    MyAppointmentsCard(appointments: viewModel.appointments)
                    AbsenseCard(absences: viewModel.absences)
                    .animation(.easeInOut(duration: 0.25), value: viewModel.absences)
                }
                .padding(.bottom, 24)
            }
            .toolbar(.hidden, for: .navigationBar)
            .refreshable { viewModel.load() }
            .overlay {
                if viewModel.isLoading && viewModel.appointments.isEmpty {
                    ProgressView()
                }
            }
            .task { viewModel.load() }
            .onChange(of: viewModel.isNFCCheckInAvailable) { _, isAvailable in
                if isAvailable && nfcController.isNFCAvailable {
                    startNFCScanning()
                } else {
                    nfcController.stop()
                }
            }
            .onDisappear { nfcController.stop() }
            .sheet(isPresented: $showAbsenceSheet) {
                ApportAbsenseView { from, until in
                    viewModel.reportAbsence(from: from, until: until)
                    showAbsenceSheet = false
                }
            }
            .alert("Backend-Fehler", isPresented: errorBinding) {
                Button("Erneut versuchen") { viewModel.load() }
                Button("OK", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "Unbekannter Fehler")
            }
        }
    }

    private func startNFCScanning() {
        guard nfcController.isNFCAvailable else { return }
        nfcController.start(
            onPayload: { cardPublicId, encryptedPiccDataHex, sdmMacHex in
                viewModel.checkInWithNFC(
                    cardPublicId: cardPublicId,
                    encryptedPiccDataHex: encryptedPiccDataHex,
                    sdmMacHex: sdmMacHex
                )
            },
            onError: viewModel.reportNFCError
        )
    }

    private var displayedCheckInStatus: CheckInStatus {
        let status = viewModel.checkInStatus
        guard !nfcController.isNFCAvailable else { return status }

        switch status {
        case .checkedIn(_), .absent(_), .error(_):
            return status
        case .notCheckedIn, .waiting(_), .nfcUnavailable, .unavailable:
            return .nfcUnavailable
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.clearError() } }
        )
    }

}

#Preview {
    StudentHomeView()
}
