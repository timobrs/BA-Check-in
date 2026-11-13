//
//  BiometricPreferenceStore.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation

protocol BiometricPreferenceStoring {
    var isBiometricUnlockEnabled: Bool { get }

    func setBiometricUnlockEnabled(_ isEnabled: Bool)
    func clear()
}

final class BiometricPreferenceStore: BiometricPreferenceStoring {
    private let defaults: UserDefaults
    private let key = "biometric_unlock_enabled"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var isBiometricUnlockEnabled: Bool {
        defaults.bool(forKey: key)
    }

    func setBiometricUnlockEnabled(_ isEnabled: Bool) {
        defaults.set(isEnabled, forKey: key)
    }

    func clear() {
        defaults.removeObject(forKey: key)
    }
}
