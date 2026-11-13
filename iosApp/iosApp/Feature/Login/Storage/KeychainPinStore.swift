//
//  KeychainPinStore.swift
//  iosApp
//
//  Created by Timo Bruns on 18.07.26.
//

import Foundation
import Security

class KeychainPinStore: PinStore {
    private let service: String = "de.jscoding.bacheckin-local-pin"
    private let account: String = "pin"
    
    private var pinQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
    
    private enum KeychainError: Error {
        case unexpectedStatus(OSStatus)
        case invalidPinData
    }
    
    func hasPin() throws -> Bool {
        return try loadPin() != nil
    }
    
    func loadPin() throws -> String? {
        var result : CFTypeRef?
        var query = pinQuery
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        query[kSecReturnData as String] = true
        
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        if status == errSecItemNotFound {
            return nil
        }
        guard status == errSecSuccess else {
            throw KeychainError.unexpectedStatus(status)
        }
        
        guard
            let pinData = result as? Data,
            let pin = String(data: pinData, encoding: .utf8)
        else {
            throw KeychainError.invalidPinData
        }

        return pin
    }
    
    func savePin(_ pin: String) throws {
        let pinData = Data(pin.utf8)
        var addQuery = pinQuery
        addQuery[kSecValueData as String] = pinData
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)

        if addStatus == errSecSuccess {
            return
        }

        guard addStatus == errSecDuplicateItem else {
            throw KeychainError.unexpectedStatus(addStatus)
        }

        let updateAttributes: [String: Any] = [
            kSecValueData as String: pinData
        ]
        
        let updateStatus = SecItemUpdate(
            pinQuery as CFDictionary,
            updateAttributes as CFDictionary
        )

        guard updateStatus == errSecSuccess else {
            throw KeychainError.unexpectedStatus(updateStatus)
        }
    }

    func clearPin() throws {
        let status = SecItemDelete(pinQuery as CFDictionary)

        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(status)
        }
    }
}
