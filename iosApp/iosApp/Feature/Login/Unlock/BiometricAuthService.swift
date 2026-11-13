//
//  BiometricAuthService.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation
import LocalAuthentication

protocol BiometricAuthenticating {
    var systemImageName: String { get }
    var accessibilityLabel: String { get }

    func authenticate(completion: @escaping (Result<Void, Error>) -> Void)
}

enum BiometricAuthError: LocalizedError {
    case unavailable
    case notEnrolled
    case locked
    case cancelled
    case failed

    var errorDescription: String? {
        switch self {
        case .unavailable:
            return "Biometrische Authentifizierung ist auf diesem Gerät nicht verfügbar."
        case .notEnrolled:
            return "Face ID oder Touch ID ist auf diesem Gerät nicht eingerichtet."
        case .locked:
            return "Die biometrische Authentifizierung ist vorübergehend gesperrt."
        case .cancelled:
            return nil
        case .failed:
            return "Die biometrische Authentifizierung ist fehlgeschlagen."
        }
    }
}

final class BiometricAuthService: BiometricAuthenticating {
    var systemImageName: String {
        switch availableBiometryType() {
        case .touchID:
            return "touchid"
        default:
            return "faceid"
        }
    }

    var accessibilityLabel: String {
        switch availableBiometryType() {
        case .touchID:
            return "Touch ID"
        default:
            return "Face ID"
        }
    }

    func authenticate(completion: @escaping (Result<Void, Error>) -> Void) {
        let context = LAContext()
        var evaluationError: NSError?

        context.localizedCancelTitle = "PIN verwenden"

        guard context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &evaluationError
        ) else {
            complete(
                .failure(map(error: evaluationError)),
                using: completion
            )
            return
        }

        context.evaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            localizedReason: "Entsperre BACheck-in mit \(accessibilityLabel)."
        ) { [weak self] success, error in
            guard let self else {
                return
            }

            if success {
                self.complete(.success(()), using: completion)
            } else {
                self.complete(
                    .failure(self.map(error: error as NSError?)),
                    using: completion
                )
            }
        }
    }

    private func availableBiometryType() -> LABiometryType {
        let context = LAContext()
        var evaluationError: NSError?
        context.canEvaluatePolicy(
            .deviceOwnerAuthenticationWithBiometrics,
            error: &evaluationError
        )
        return context.biometryType
    }

    private func map(error: NSError?) -> BiometricAuthError {
        guard
            let error,
            error.domain == LAError.errorDomain,
            let code = LAError.Code(rawValue: error.code)
        else {
            return .failed
        }

        switch code {
        case .biometryNotAvailable:
            return .unavailable
        case .biometryNotEnrolled:
            return .notEnrolled
        case .biometryLockout:
            return .locked
        case .userCancel, .userFallback, .systemCancel, .appCancel:
            return .cancelled
        default:
            return .failed
        }
    }

    private func complete(
        _ result: Result<Void, Error>,
        using completion: @escaping (Result<Void, Error>) -> Void
    ) {
        DispatchQueue.main.async {
            completion(result)
        }
    }
}
