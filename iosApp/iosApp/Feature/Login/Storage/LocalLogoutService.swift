//
//  LocalLogoutService.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation

final class LocalLogoutService {
    private let pinStore: any PinStore
    private let biometricPreferenceStore: any BiometricPreferenceStoring
    private let bindingStore: DeviceBindingStore
    private let backendSession: BackendSession

    init(
        pinStore: any PinStore = KeychainPinStore(),
        biometricPreferenceStore: any BiometricPreferenceStoring = BiometricPreferenceStore(),
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        backendSession: BackendSession = .shared
    ) {
        self.pinStore = pinStore
        self.biometricPreferenceStore = biometricPreferenceStore
        self.bindingStore = bindingStore
        self.backendSession = backendSession
    }

    func logout(completion: @escaping (Result<Void, Error>) -> Void) {
        backendSession.prepareAuthenticatedBackend { [weak self] error in
            guard let self else { return }
            if let error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            self.backendSession.backend.unlinkCurrentDevice { error in
                if let error {
                    DispatchQueue.main.async { completion(.failure(error)) }
                    return
                }
                do {
                    try self.clearLocalDevice()
                    DispatchQueue.main.async { completion(.success(())) }
                } catch {
                    DispatchQueue.main.async { completion(.failure(error)) }
                }
            }
        }
    }

    func clearLocalDevice() throws {
        var firstError: Error?

        do {
            try pinStore.clearPin()
        } catch {
            firstError = error
        }

        do {
            try backendSession.clearLocalCredentials()
        } catch {
            if firstError == nil {
                firstError = error
            }
        }

        biometricPreferenceStore.clear()
        bindingStore.clear()

        if let firstError {
            throw firstError
        }
    }
}
