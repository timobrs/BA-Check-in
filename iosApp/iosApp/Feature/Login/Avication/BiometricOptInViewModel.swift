//
//  BiometricOptInViewModel.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation
import SwiftUI

final class BiometricOptInViewModel: ObservableObject {
    @Published private(set) var isBiometricUnlockEnabled: Bool
    @Published var isAuthenticating = false
    @Published var errorMessage: String?
    @Published var isCompleted = false

    private let biometricAuthService: any BiometricAuthenticating
    private let preferenceStore: any BiometricPreferenceStoring

    var biometricSystemImageName: String {
        biometricAuthService.systemImageName
    }

    var biometricName: String {
        biometricAuthService.accessibilityLabel
    }

    init(
        biometricAuthService: any BiometricAuthenticating = BiometricAuthService(),
        preferenceStore: any BiometricPreferenceStoring = BiometricPreferenceStore()
    ) {
        self.biometricAuthService = biometricAuthService
        self.preferenceStore = preferenceStore
        isBiometricUnlockEnabled = preferenceStore.isBiometricUnlockEnabled
    }

    func enableBiometrics() {
        guard !isAuthenticating else {
            return
        }

        isAuthenticating = true
        errorMessage = nil

        biometricAuthService.authenticate { [weak self] result in
            guard let self else {
                return
            }

            self.isAuthenticating = false

            switch result {
            case .success:
                self.preferenceStore.setBiometricUnlockEnabled(true)
                self.isBiometricUnlockEnabled = true
                self.isCompleted = true
            case .failure(let error):
                self.isBiometricUnlockEnabled = false

                if let biometricError = error as? BiometricAuthError,
                    case .cancelled = biometricError {
                    return
                }

                self.errorMessage = error.localizedDescription
            }
        }
    }

    func disableBiometrics() {
        preferenceStore.setBiometricUnlockEnabled(false)
        isBiometricUnlockEnabled = false
    }

    func skipBiometrics() {
        preferenceStore.setBiometricUnlockEnabled(false)
        isBiometricUnlockEnabled = false
        isCompleted = true
    }
    
    func setBiometricsEnabled(_ isEnabled: Bool) {
        if isEnabled {
            enableBiometrics()
        } else {
            disableBiometrics()
        }
    }
}
