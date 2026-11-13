//
//  AppStartView.swift
//  iosApp
//
//  Created by Timo Bruns on 20.07.26.
//

import SwiftUI

struct AppStartView: View {
    private let bindingStore: DeviceBindingStore
    private let localLogoutService: LocalLogoutService
    @State private var binding: DeviceBindingState
    @State private var rootFlowID = UUID()
    @State private var continueAfterActivation = false
    @State private var isResettingRevokedDevice = false
    @State private var resetErrorMessage: String?

    init(
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        localLogoutService: LocalLogoutService? = nil
    ) {
        self.bindingStore = bindingStore
        self.localLogoutService = localLogoutService
            ?? LocalLogoutService(bindingStore: bindingStore)
        _binding = State(initialValue: bindingStore.load())
    }

    var body: some View {
        Group {
            switch binding.status {
            case .unactivated:
                Avication1View(onActivationCompleted: handleActivationCompleted)

            case .activated:
                if binding.hasActiveLocalBinding {
                    if continueAfterActivation {
                        HomeContainerView(onLogout: handleLogout)
                    } else {
                        UnlockView(onLogout: handleLogout)
                    }
                } else {
                    ContentUnavailableView(
                        "Unvollständige Geräteaktivierung",
                        systemImage: "exclamationmark.triangle",
                        description: Text("Die Geräte-Credential-ID fehlt. Bitte aktiviere das Gerät erneut.")
                    )
                }

            case .revoked:
                ContentUnavailableView {
                    Label("Gerät widerrufen", systemImage: "xmark.shield")
                } description: {
                    Text("Dieses Gerät wurde deaktiviert. Für eine erneute Nutzung brauchst du einen neuen Freischaltungscode aus dem Sekretariat.")
                } actions: {
                    Button(action: handleRevokedDeviceReset) {
                        HStack(spacing: 8) {
                            if isResettingRevokedDevice {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Image(systemName: "arrow.counterclockwise")
                            }
                            Text("Erneut freischalten")
                        }
                        .baFullWidthActionLabel()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.baPrimary)
                    .frame(maxWidth: .infinity)
                    .disabled(isResettingRevokedDevice)
                }
            }
        }
        .id(rootFlowID)
        .onAppear(perform: reloadBinding)
        .alert("Zurücksetzen fehlgeschlagen", isPresented: resetErrorBinding) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(resetErrorMessage ?? "Die lokalen Gerätedaten konnten nicht gelöscht werden.")
        }
    }

    private func reloadBinding() {
        binding = bindingStore.load()
    }

    private func handleLogout() {
        continueAfterActivation = false
        reloadBinding()
        rootFlowID = UUID()
    }

    private func handleActivationCompleted() {
        reloadBinding()
        guard binding.hasActiveLocalBinding else { return }
        continueAfterActivation = true
        rootFlowID = UUID()
    }

    private func handleRevokedDeviceReset() {
        guard !isResettingRevokedDevice else { return }
        isResettingRevokedDevice = true

        do {
            try localLogoutService.clearLocalDevice()
            continueAfterActivation = false
            reloadBinding()
            rootFlowID = UUID()
        } catch {
            resetErrorMessage = error.localizedDescription
        }

        isResettingRevokedDevice = false
    }

    private var resetErrorBinding: Binding<Bool> {
        Binding(
            get: { resetErrorMessage != nil },
            set: { if !$0 { resetErrorMessage = nil } }
        )
    }
}

#Preview {
    AppStartView()
}
