//
//  Avication3PinValidate.swift
//  iosApp
//
//  Created by Timo Bruns on 13.07.26.
//

import SwiftUI

struct Avication3PinValidate: View {
    @ObservedObject var avication: AvicationViewModel
    var onActivationCompleted: () -> Void = {}
    
    var body: some View {
        PinPadView(
            pin: $avication.confirmPIN,
            pinInfoText: "Bestätige deine lokale PIN",
            pinInfoHeadline: "Lokale PIN",
            showFaceIdButton: false
        )
        .onChange(of: avication.confirmPIN) { _, pin in
            if pin.count == 5 {
                avication.completeActivation()
            }
        }
        .navigationDestination(item: $avication.activationResult) { result in
            AvicationAllowBiometricView(
                result: result,
                onActivationCompleted: onActivationCompleted
            )
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 8) {
                if avication.isActivating {
                    ProgressView("Gerät wird aktiviert ...")
                }

                if let errorMessage = avication.pinErrorMessage
                    ?? avication.activationErrorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 8)
        }
        .allowsHitTesting(!avication.isActivating)
    }
}

#Preview {
    Avication3PinValidate(avication: AvicationViewModel())
}
