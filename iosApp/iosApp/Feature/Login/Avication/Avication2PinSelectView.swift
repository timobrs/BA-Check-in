//
//  Avication2PinSelectView.swift
//  iosApp
//
//  Created by Timo Bruns on 13.07.26.
//

import SwiftUI

struct Avication2PinSelectView: View {
    @ObservedObject var avication: AvicationViewModel
    @State private var showPinConfirmation = false
    var onActivationCompleted: () -> Void = {}
    
    var body: some View {
        PinPadView(
            pin: $avication.selectPIN,
            pinInfoText: "Wähle einen lokalen PIN aus",
            pinInfoHeadline: "Lokale PIN",
            showFaceIdButton: false
        )
        .onChange(of: avication.selectPIN) { _, pin in
            if pin.count == 5 {
                showPinConfirmation = true
            }
        }
        .navigationDestination(isPresented: $showPinConfirmation) {
            Avication3PinValidate(
                avication: avication,
                onActivationCompleted: onActivationCompleted
            )
        }
    }
}

#Preview {
    Avication2PinSelectView(avication: AvicationViewModel())
}
