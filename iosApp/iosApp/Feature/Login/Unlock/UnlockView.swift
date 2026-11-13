//
//  UnlockView.swift
//  iosApp
//
//  Created by Timo Bruns on 14.07.26.
//

import SwiftUI

struct UnlockView: View {
    @StateObject private var unlockViewModel = UnlockViewModel()
    var onLogout: () -> Void = {}

    var body: some View {
        Group {
            if unlockViewModel.isUnlocked {
                HomeContainerView(onLogout: onLogout)
            } else {
                NavigationStack {
                    PinPadView(
                        pin: $unlockViewModel.enteredPin,
                        pinInfoText: "Gib deine PIN ein oder nutze deine biometrischen Daten, um die App zu entsperren.",
                        pinInfoHeadline: "App entsperren",
                        showFaceIdButton: unlockViewModel.isBiometricUnlockEnabled,
                        biometricSystemImageName: unlockViewModel.biometricSystemImageName,
                        biometricAccessibilityLabel: unlockViewModel.biometricAccessibilityLabel,
                        onBiometricAuthentication: unlockViewModel.unlockWithBiometrics
                    )
                    .onChange(of: unlockViewModel.enteredPin) { _, pin in
                        if pin.count == 5 {
                            unlockViewModel.unlockDeviceWithPin()
                        }
                    }
                    .safeAreaInset(edge: .bottom) {
                        if let errorMessage = unlockViewModel.pinErrorMessage {
                            Text(errorMessage)
                                .font(.footnote)
                                .foregroundStyle(.red)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                                .padding(.bottom, 8)
                        }
                    }
                }
            }
        }
        .task {
            unlockViewModel.unlockWithBiometricsAutomatically()
        }
    }
}

#Preview {
    UnlockView()
}
