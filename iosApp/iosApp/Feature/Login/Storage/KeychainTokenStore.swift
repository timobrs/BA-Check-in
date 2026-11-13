//
//  KeychainTokenStore.swift
//  iosApp
//
//  Created by Timo Bruns on 23.07.26.
//

import Foundation
import Security

final class KeychainTokenStore: TokenStore {
    private let service = "de.jscoding.bacheckin.tokens"
    private let account = "device-session"

    private var tokenQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }

    private enum KeychainError: Error {
        case unexpectedStatus(OSStatus)
        case invalidTokenData
    }

    func save(_ tokens: StoredTokens) throws {
        let tokenData = try JSONEncoder().encode(tokens)
        var addQuery = tokenQuery
        addQuery[kSecValueData as String] = tokenData
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly

        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
        if addStatus == errSecSuccess {
            return
        }

        guard addStatus == errSecDuplicateItem else {
            throw KeychainError.unexpectedStatus(addStatus)
        }

        let updateAttributes: [String: Any] = [
            kSecValueData as String: tokenData
        ]
        let updateStatus = SecItemUpdate(
            tokenQuery as CFDictionary,
            updateAttributes as CFDictionary
        )

        guard updateStatus == errSecSuccess else {
            throw KeychainError.unexpectedStatus(updateStatus)
        }
    }

    func load() throws -> StoredTokens? {
        var result: CFTypeRef?
        var query = tokenQuery
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        query[kSecReturnData as String] = true

        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound {
            return nil
        }

        guard status == errSecSuccess, let tokenData = result as? Data else {
            throw KeychainError.unexpectedStatus(status)
        }

        guard let tokens = try? JSONDecoder().decode(StoredTokens.self, from: tokenData) else {
            throw KeychainError.invalidTokenData
        }

        return tokens
    }

    func clear() throws {
        let status = SecItemDelete(tokenQuery as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(status)
        }
    }
}
