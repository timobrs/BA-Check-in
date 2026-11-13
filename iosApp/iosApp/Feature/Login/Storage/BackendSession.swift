import CryptoKit
import Foundation
import Security
import Shared

final class BackendSession {
    static let shared = BackendSession()

    let backend: BackendRemoteDataSourceImpl

    private let tokenStore: any TokenStore
    private let bindingStore: DeviceBindingStore
    private let deviceKeyStore: DeviceKeyStore
    private let lock = NSLock()
    private var isRefreshing = false
    private var refreshWaiters: [(Error?) -> Void] = []

    init(
        baseURL: String = BackendEnvironment.baseURL,
        tokenStore: any TokenStore = KeychainTokenStore(),
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        deviceKeyStore: DeviceKeyStore = DeviceKeyStore()
    ) {
        let config = BackendRuntimeConfig(mode: .postgresApi, baseUrl: baseURL)
        let postgresBackend = PostgresBackendRemoteDataSource(config: config)
        self.backend = BackendRemoteDataSourceImpl(
            config: config,
            postgresBackendRemoteDataSource: postgresBackend
        )
        self.tokenStore = tokenStore
        self.bindingStore = bindingStore
        self.deviceKeyStore = deviceKeyStore

        if let accessToken = try? tokenStore.load()?.accessToken {
            backend.setAccessToken(accessToken: accessToken)
        }
    }

    func prepareAuthenticatedBackend(completion: @escaping (Error?) -> Void) {
        let tokens: StoredTokens
        do {
            guard let storedTokens = try tokenStore.load() else {
                completion(BackendSessionError.missingTokens)
                return
            }
            tokens = storedTokens
        } catch {
            completion(error)
            return
        }

        let refreshBuffer: Int64 = 30_000
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        guard now >= tokens.accessTokenExpiresAtEpochMillis - refreshBuffer else {
            backend.setAccessToken(accessToken: tokens.accessToken)
            completion(nil)
            return
        }

        lock.lock()
        refreshWaiters.append(completion)
        if isRefreshing {
            lock.unlock()
            return
        }
        isRefreshing = true
        lock.unlock()

        refresh(tokens: tokens) { [weak self] error in
            guard let self else { return }
            self.lock.lock()
            let waiters = self.refreshWaiters
            self.refreshWaiters.removeAll()
            self.isRefreshing = false
            self.lock.unlock()
            waiters.forEach { $0(error) }
        }
    }

    func prepareAuthenticatedBackend() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            prepareAuthenticatedBackend { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    func openDeviceSession(
        deviceCredentialId: String,
        completion: @escaping (Result<BackendResponses.DeviceSession, Error>) -> Void
    ) {
        backend.createAuthChallenge(
            request: BackendRequests.AuthChallenge(deviceCredentialId: deviceCredentialId)
        ) { [weak self] challenge, error in
            guard let self else { return }
            if let error {
                completion(.failure(error))
                return
            }
            guard let challenge else {
                completion(.failure(BackendSessionError.missingResponse))
                return
            }

            do {
                let timestamp = Int64(Date().timeIntervalSince1970 * 1_000)
                let payload = [
                    "BACHECKIN_DEVICE_SESSION_V1",
                    deviceCredentialId,
                    challenge.authSessionId,
                    challenge.challengeBase64,
                    String(timestamp)
                ].joined(separator: "\n")
                let signature = try self.deviceKeyStore.sign(Data(payload.utf8))
                let request = BackendRequests.DeviceSession(
                    deviceCredentialId: deviceCredentialId,
                    authSessionId: challenge.authSessionId,
                    timestampEpochMillis: timestamp,
                    signatureBase64: signature
                )

                self.backend.openDeviceSession(request: request) { response, error in
                    if let error {
                        self.handleAuthenticationFailure(error)
                        completion(.failure(error))
                    } else if let response {
                        do {
                            try self.save(session: response)
                            completion(.success(response))
                        } catch {
                            completion(.failure(error))
                        }
                    } else {
                        completion(.failure(BackendSessionError.missingResponse))
                    }
                }
            } catch {
                completion(.failure(error))
            }
        }
    }

    func clearLocalCredentials() throws {
        var firstError: Error?
        do {
            try tokenStore.clear()
        } catch {
            firstError = error
        }
        do {
            try deviceKeyStore.deleteKey()
        } catch {
            if firstError == nil { firstError = error }
        }
        backend.setAccessToken(accessToken: nil)
        if let firstError { throw firstError }
    }

    private func refresh(tokens: StoredTokens, completion: @escaping (Error?) -> Void) {
        guard let refreshToken = tokens.refreshToken,
              let deviceCredentialId = bindingStore.load().deviceCredentialId else {
            completion(BackendSessionError.missingRefreshData)
            return
        }

        do {
            let timestamp = Int64(Date().timeIntervalSince1970 * 1_000)
            let refreshHash = Data(SHA256.hash(data: Data(refreshToken.utf8))).base64URLEncodedString()
            let payload = [
                "BACHECKIN_DEVICE_REFRESH_V1",
                deviceCredentialId,
                refreshHash,
                String(timestamp)
            ].joined(separator: "\n")
            let signature = try deviceKeyStore.sign(Data(payload.utf8))
            let request = BackendRequests.Refresh(
                deviceCredentialId: deviceCredentialId,
                refreshToken: refreshToken,
                timestampEpochMillis: timestamp,
                signatureBase64: signature
            )

            backend.refresh(request: request) { [weak self] response, error in
                guard let self else { return }
                if let error {
                    self.handleAuthenticationFailure(error)
                    completion(error)
                    return
                }
                guard let response else {
                    completion(BackendSessionError.missingResponse)
                    return
                }
                do {
                    try self.tokenStore.save(
                        StoredTokens(
                            accessToken: response.accessToken,
                            refreshToken: response.refreshToken ?? refreshToken,
                            accessTokenExpiresAtEpochMillis: response.accessTokenExpiresAtEpochMillis
                        )
                    )
                    self.bindingStore.updateTokenExpiry(response.accessTokenExpiresAtEpochMillis)
                    self.backend.setAccessToken(accessToken: response.accessToken)
                    completion(nil)
                } catch {
                    completion(error)
                }
            }
        } catch {
            completion(error)
        }
    }

    private func save(session: BackendResponses.DeviceSession) throws {
        try tokenStore.save(
            StoredTokens(
                accessToken: session.accessToken,
                refreshToken: session.refreshToken,
                accessTokenExpiresAtEpochMillis: session.accessTokenExpiresAtEpochMillis
            )
        )
        backend.setAccessToken(accessToken: session.accessToken)
    }

    private func handleAuthenticationFailure(_ error: Error) {
        guard let backendError = (error as NSError).kotlinException as? BackendException else {
            return
        }
        let revokedCodes = [
            BackendDeviceErrorCode.deviceRevoked,
            BackendDeviceErrorCode.deviceNotActive,
            BackendDeviceErrorCode.deviceReplaced
        ]
        if revokedCodes.contains(where: { backendError.code.isEqual($0) }) {
            try? tokenStore.clear()
            backend.setAccessToken(accessToken: nil)
            bindingStore.setRevoked()
        }
    }
}

enum BackendEnvironment {
    static var baseURL: String {
        if let override = ProcessInfo.processInfo.environment["BACKEND_BASE_URL"],
           !override.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return override
        }
        return "https://bacheckin.brettspielrunde.com"
    }
}

enum BackendSessionError: LocalizedError {
    case missingTokens
    case missingRefreshData
    case missingResponse

    var errorDescription: String? {
        switch self {
        case .missingTokens:
            return "Es sind keine Anmeldedaten gespeichert."
        case .missingRefreshData:
            return "Die Gerätesitzung kann nicht erneuert werden."
        case .missingResponse:
            return "Das Backend hat keine Daten zurückgegeben."
        }
    }
}

final class DeviceKeyStore {
    private let service = "de.jscoding.bacheckin.device-key"
    private let account = "device-identity-v1"

    private var query: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    func generateKey() throws -> String {
        let key = P256.Signing.PrivateKey()
        try save(key.rawRepresentation)
        return Self.subjectPublicKeyInfo(for: key.publicKey).base64EncodedString()
    }

    func sign(_ payload: Data) throws -> String {
        let key = try loadKey()
        return try key.signature(for: payload).derRepresentation.base64EncodedString()
    }

    func deleteKey() throws {
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw DeviceKeyStoreError.keychain(status)
        }
    }

    private func save(_ data: Data) throws {
        try deleteKey()
        var addQuery = query
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let status = SecItemAdd(addQuery as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw DeviceKeyStoreError.keychain(status)
        }
    }

    private func loadKey() throws -> P256.Signing.PrivateKey {
        var result: CFTypeRef?
        var loadQuery = query
        loadQuery[kSecMatchLimit as String] = kSecMatchLimitOne
        loadQuery[kSecReturnData as String] = true
        let status = SecItemCopyMatching(loadQuery as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else {
            throw DeviceKeyStoreError.keychain(status)
        }
        return try P256.Signing.PrivateKey(rawRepresentation: data)
    }

    private static func subjectPublicKeyInfo(for publicKey: P256.Signing.PublicKey) -> Data {
        // ASN.1 SubjectPublicKeyInfo prefix for id-ecPublicKey on prime256v1.
        let prefix: [UInt8] = [
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86, 0x48, 0xCE,
            0x3D, 0x02, 0x01, 0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D,
            0x03, 0x01, 0x07, 0x03, 0x42, 0x00
        ]
        return Data(prefix) + publicKey.x963Representation
    }
}

enum DeviceKeyStoreError: Error {
    case keychain(OSStatus)
}

extension Data {
    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
