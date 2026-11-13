//
//  TokenStore.swift
//  iosApp
//
//  Created by Timo Bruns on 23.07.26.
//

import Foundation

struct StoredTokens: Codable, Equatable {
    let accessToken: String
    let refreshToken: String?
    let accessTokenExpiresAtEpochMillis: Int64
}

protocol TokenStore {
    func save(_ tokens: StoredTokens) throws
    func load() throws -> StoredTokens?
    func clear() throws
}
