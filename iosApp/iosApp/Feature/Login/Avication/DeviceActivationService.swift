//
//  DeviceActivationService.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import Foundation
import Shared
import CryptoKit
import UIKit

struct ActivationResult: Hashable, Identifiable {
    let userId: String
    let displayName: String
    let role: String

    var id: String {
        userId
    }
}

enum DeviceActivationError: LocalizedError {
    case missingChallenge
    case missingResponse
    case storageFailed(Error)

    var errorDescription: String? {
        switch self {
        case .missingChallenge:
            return "Die Aktivierungs-Challenge konnte nicht geladen werden."
        case .missingResponse:
            return "Das Backend hat keine Aktivierungsdaten zurückgegeben."
        case .storageFailed:
            return "Die Aktivierungsdaten konnten nicht sicher gespeichert werden."
        }
    }
}

final class DeviceActivationService {
    private let backendSession: BackendSession
    private let pinStore: any PinStore
    private let bindingStore: DeviceBindingStore
    private let deviceKeyStore: DeviceKeyStore

    init(
        backendSession: BackendSession = .shared,
        pinStore: any PinStore = KeychainPinStore(),
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        deviceKeyStore: DeviceKeyStore = DeviceKeyStore()
    ) {
        self.backendSession = backendSession
        self.pinStore = pinStore
        self.bindingStore = bindingStore
        self.deviceKeyStore = deviceKeyStore
    }

    func activate(
        registrationKey: String,
        pin: String,
        completion: @escaping (Result<ActivationResult, Error>) -> Void
    ) {
        let backend = backendSession.backend
        backend.validateActivationCode(activationCode: registrationKey) { [weak self] error in
            guard let self else {
                return
            }
            if let error {
                self.complete(.failure(error), using: completion)
                return
            }
            backend.createActivationChallenge { challenge, error in
                if let error {
                    self.complete(.failure(error), using: completion)
                    return
                }
                guard let challenge else {
                    self.complete(.failure(DeviceActivationError.missingChallenge), using: completion)
                    return
                }

                do {
                    let publicKeyBase64 = try self.deviceKeyStore.generateKey()
                    let publicKeyData = Data(base64Encoded: publicKeyBase64) ?? Data()
                    let fingerprint = Data(SHA256.hash(data: publicKeyData)).base64URLEncodedString()
                    let proof = [
                        "BACHECKIN_DEVICE_ACTIVATION_V1",
                        challenge.activationSessionId,
                        challenge.challengeBase64.trimmingCharacters(in: CharacterSet(charactersIn: "=")),
                        fingerprint
                    ].joined(separator: "\n")
                    let request = BackendRequests.CompleteActivation(
                        activationSessionId: challenge.activationSessionId,
                        activationCode: registrationKey,
                        publicKeyBase64: publicKeyBase64,
                        challengeSignatureBase64: try self.deviceKeyStore.sign(Data(proof.utf8)),
                        deviceInfo: self.deviceInfo()
                    )

                    backend.completeActivation(request: request) { response, error in
                        if let error {
                            self.rollbackLocalActivation()
                            self.complete(.failure(error), using: completion)
                            return
                        }
                        guard let response else {
                            self.rollbackLocalActivation()
                            self.complete(.failure(DeviceActivationError.missingResponse), using: completion)
                            return
                        }
                        self.openInitialSession(response: response, pin: pin, completion: completion)
                    }
                } catch {
                    self.rollbackLocalActivation()
                    self.complete(.failure(error), using: completion)
                }
            }
        }
    }

    private func openInitialSession(
        response: BackendResponses.CompleteActivation,
        pin: String,
        completion: @escaping (Result<ActivationResult, Error>) -> Void
    ) {
        backendSession.openDeviceSession(deviceCredentialId: response.deviceCredentialId) { [weak self] result in
            guard let self else { return }
            switch result {
            case .failure(let error):
                self.rollbackLocalActivation()
                self.complete(.failure(error), using: completion)
            case .success(let session):
                do {
                    let activation = try self.persist(response: response, session: session, pin: pin)
                    self.complete(.success(activation), using: completion)
                } catch {
                    self.rollbackLocalActivation()
                    self.complete(.failure(DeviceActivationError.storageFailed(error)), using: completion)
                }
            }
        }
    }

    private func persist(
        response: BackendResponses.CompleteActivation,
        session: BackendResponses.DeviceSession,
        pin: String
    ) throws -> ActivationResult {
        try pinStore.savePin(pin)

        let role = response.userRole.name
        bindingStore.setActivated(
            userId: response.userId,
            userRole: role,
            displayName: response.displayName,
            deviceCredentialId: response.deviceCredentialId,
            accessTokenExpiresAtEpochMillis: session.accessTokenExpiresAtEpochMillis
        )

        return ActivationResult(
            userId: response.userId,
            displayName: response.displayName ?? "Unbekannt",
            role: role
        )
    }

    private func rollbackLocalActivation() {
        try? backendSession.clearLocalCredentials()
        try? pinStore.clearPin()
        bindingStore.clear()
    }

    private func complete(
        _ result: Result<ActivationResult, Error>,
        using completion: @escaping (Result<ActivationResult, Error>) -> Void
    ) {
        DispatchQueue.main.async {
            completion(result)
        }
    }

    private func deviceInfo() -> BackendDtos.DeviceInfo {
        let versionName = Bundle.main.object(
            forInfoDictionaryKey: "CFBundleShortVersionString"
        ) as? String ?? "0"
        let buildString = Bundle.main.object(
            forInfoDictionaryKey: "CFBundleVersion"
        ) as? String ?? "0"

        return BackendDtos.DeviceInfo(
            appVersionName: versionName,
            appVersionCode: Int64(buildString) ?? 0,
            androidSdk: 0,
            manufacturer: "Apple",
            model: UIDevice.current.model
        )
    }
}
