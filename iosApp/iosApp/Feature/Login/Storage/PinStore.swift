//
//  PinStore.swift
//  iosApp
//
//  Created by Timo Bruns on 18.07.26.
//

protocol PinStore{
    func hasPin() throws -> Bool
    
    func loadPin() throws -> String?

    func savePin(_ pin: String) throws

    func clearPin() throws
}
