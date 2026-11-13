//
//  AvicationViewModel.swift
//  iosApp
//
//  Created by Timo Bruns on 14.07.26.
//

import Foundation
import SwiftUI

class AvicationViewModel: ObservableObject {
    @Published var registrationKey: String = ""
    @Published var selectPIN: String = ""
    @Published var confirmPIN: String = ""
    @Published var pinErrorMessage: String?
    @Published var activationErrorMessage: String?
    @Published var isActivating = false
    @Published var activationResult: ActivationResult?

    private let activationService: DeviceActivationService

    init(activationService: DeviceActivationService = DeviceActivationService()) {
        self.activationService = activationService
    }
    
    func validatePIN() -> Bool {
        guard selectPIN.count == 5, confirmPIN.count == 5 else {
            return false
        }

        guard selectPIN == confirmPIN else {
            pinErrorMessage = "Die PINs stimmen nicht überein. Bitte versuche es erneut."
            confirmPIN = ""
            return false
        }
        pinErrorMessage = nil
        return true
    }

    func completeActivation() {
        guard validatePIN(), !isActivating else {
            return
        }

        isActivating = true
        activationErrorMessage = nil
        let code = registrationKey.trimmingCharacters(in: .whitespacesAndNewlines)

        activationService.activate(registrationKey: code, pin: selectPIN) { [weak self] result in
            guard let self else {
                return
            }

            self.isActivating = false

            switch result {
            case .success(let activationResult):
                self.activationResult = activationResult
            case .failure(let error):
                self.activationErrorMessage = error.localizedDescription
            }
        }
    }
}
