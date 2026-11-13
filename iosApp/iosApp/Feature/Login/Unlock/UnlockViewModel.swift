//
//  UnlockViewModel.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation
import SwiftUI

class UnlockViewModel: ObservableObject {
    @Published var enteredPin: String = ""
    @Published var pinErrorMessage: String?
    @Published var isUnlocked: Bool = false

    private let pinStore: any PinStore
    private let biometricAuthService: any BiometricAuthenticating
    private let biometricPreferenceStore: any BiometricPreferenceStoring
    private var hasAttemptedAutomaticBiometricUnlock = false

    var isBiometricUnlockEnabled: Bool {
        biometricPreferenceStore.isBiometricUnlockEnabled
    }

    var biometricSystemImageName: String {
        biometricAuthService.systemImageName
    }

    var biometricAccessibilityLabel: String {
        biometricAuthService.accessibilityLabel
    }

    init(
        pinStore: any PinStore = KeychainPinStore(),
        biometricAuthService: any BiometricAuthenticating = BiometricAuthService(),
        biometricPreferenceStore: any BiometricPreferenceStoring = BiometricPreferenceStore()
    ) {
        self.pinStore = pinStore
        self.biometricAuthService = biometricAuthService
        self.biometricPreferenceStore = biometricPreferenceStore
    }

    func unlockDeviceWithPin() {
        guard enteredPin.count == 5 else {
            pinErrorMessage = "Die PIN muss aus genau 5 Ziffern bestehen."
            return
        }

        do {
            guard let savedPin = try pinStore.loadPin() else {
                pinErrorMessage = "Auf diesem Gerät ist keine PIN gespeichert."
                return
            }

            guard enteredPin == savedPin else {
                pinErrorMessage = "Die eingegebene PIN ist nicht korrekt."
                enteredPin = ""
                return
            }

            pinErrorMessage = nil
            isUnlocked = true
        } catch {
            pinErrorMessage = "Die gespeicherte PIN konnte nicht geladen werden."
        }
    }

    func unlockWithBiometrics() {
        guard isBiometricUnlockEnabled else {
            return
        }

        pinErrorMessage = nil

        biometricAuthService.authenticate { [weak self] result in
            guard let self else {
                return
            }

            switch result {
            case .success:
                self.isUnlocked = true
            case .failure(let error):
                if let biometricError = error as? BiometricAuthError,
                    case .cancelled = biometricError {
                    return
                }

                self.pinErrorMessage = error.localizedDescription
            }
        }
    }

    func unlockWithBiometricsAutomatically() {
        guard !hasAttemptedAutomaticBiometricUnlock,
              isBiometricUnlockEnabled else {
            return
        }

        hasAttemptedAutomaticBiometricUnlock = true
        unlockWithBiometrics()
    }
}
