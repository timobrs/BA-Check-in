//
//  AvicationAllowBiometricView.swift
//  iosApp
//
//  Created by Timo Bruns on 30.07.26.
//

import SwiftUI

struct AvicationAllowBiometricView: View {
    let result: ActivationResult
    @StateObject private var viewModel = BiometricOptInViewModel()
    var onActivationCompleted: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .center, spacing: 16) {
                Spacer()
                Image(systemName: viewModel.biometricSystemImageName)
                    .font(.system(size: 64, weight: .medium))
                    .foregroundStyle(Color.baPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 32)
                Text("Mit Face ID schneller einchecken")
                    .font(.largeTitle.bold())
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)

                Text("Entsperre den BA Checkin schnell uns sicher. Deine biometrischen Daten bleiben geschützt auf diesem Gerät. Du kannst weiterhin jederzeit deine lokale PIN verwenden.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 4)
                Spacer()
            }

            Spacer()

            VStack(spacing: 10) {
                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 4)
                }

                Button(action: viewModel.enableBiometrics) {
                    HStack {
                        if viewModel.isAuthenticating {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Label(
                                "Biometrie verwenden",
                                systemImage: viewModel.biometricSystemImageName
                            )
                        }
                    }
                    .fontWeight(.semibold)
                    .baFullWidthActionLabel()
                    .background(Color.baPrimary)
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(viewModel.isAuthenticating)

                Button("Nicht jetzt", action: viewModel.skipBiometrics)
                    .fontWeight(.medium)
                    .foregroundStyle(.primary)
                    .baFullWidthActionLabel()
                    .buttonStyle(.plain)
                    .disabled(viewModel.isAuthenticating)
            }
        }
        .padding(24)
        .navigationBarBackButtonHidden(true)
        .onChange(of: viewModel.isCompleted) { _, isCompleted in
            if isCompleted {
                onActivationCompleted()
            }
        }
    }
}

#Preview {
    NavigationStack {
        AvicationAllowBiometricView(
            result: ActivationResult(
                userId: "demo-user",
                displayName: "Demo User",
                role: "STUDENT"
            )
        )
    }
}
