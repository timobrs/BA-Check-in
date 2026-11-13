//
//  DeviceBindingStore.swift
//  iosApp
//
//  Created by Timo Bruns on 20.07.26.
//

import Foundation

enum DeviceBindingStatus: String {
    case unactivated
    case activated
    case revoked
}

struct DeviceBindingState {
    let status: DeviceBindingStatus
    let userId: String?
    let userRole: String?
    let displayName: String?
    let deviceCredentialId: String?
    let accessTokenExpiresAtEpochMillis: Int64?

    var hasActiveLocalBinding: Bool {
        status == .activated && !(deviceCredentialId?.isEmpty ?? true)
    }
}

final class DeviceBindingStore {
    private enum Key {
        static let status = "device_binding_status"
        static let userId = "device_binding_user_id"
        static let userRole = "device_binding_user_role"
        static let displayName = "device_binding_display_name"
        static let deviceCredentialId = "device_binding_credential_id"
        static let accessTokenExpiresAt = "device_binding_access_token_expires_at"
    }

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> DeviceBindingState {
        let status = defaults.string(forKey: Key.status)
            .flatMap(DeviceBindingStatus.init(rawValue:)) ?? .unactivated
        let expiry = defaults.object(forKey: Key.accessTokenExpiresAt) as? NSNumber

        return DeviceBindingState(
            status: status,
            userId: defaults.string(forKey: Key.userId),
            userRole: defaults.string(forKey: Key.userRole),
            displayName: defaults.string(forKey: Key.displayName),
            deviceCredentialId: defaults.string(forKey: Key.deviceCredentialId),
            accessTokenExpiresAtEpochMillis: expiry?.int64Value
        )
    }

    func setActivated(
        userId: String,
        userRole: String,
        displayName: String?,
        deviceCredentialId: String,
        accessTokenExpiresAtEpochMillis: Int64
    ) {
        defaults.set(DeviceBindingStatus.activated.rawValue, forKey: Key.status)
        defaults.set(userId, forKey: Key.userId)
        defaults.set(userRole, forKey: Key.userRole)
        defaults.set(displayName, forKey: Key.displayName)
        defaults.set(deviceCredentialId, forKey: Key.deviceCredentialId)
        defaults.set(accessTokenExpiresAtEpochMillis, forKey: Key.accessTokenExpiresAt)
    }

    func setRevoked() {
        defaults.set(DeviceBindingStatus.revoked.rawValue, forKey: Key.status)
        defaults.removeObject(forKey: Key.accessTokenExpiresAt)
    }

    func updateTokenExpiry(_ accessTokenExpiresAtEpochMillis: Int64) {
        defaults.set(accessTokenExpiresAtEpochMillis, forKey: Key.accessTokenExpiresAt)
    }

    func clear() {
        [
            Key.status,
            Key.userId,
            Key.userRole,
            Key.displayName,
            Key.deviceCredentialId,
            Key.accessTokenExpiresAt
        ].forEach(defaults.removeObject(forKey:))
    }
}
