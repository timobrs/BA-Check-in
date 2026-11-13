//
//  PinPadView.swift
//  iosApp
//
//  Created by Timo Bruns on 09.07.26.
//

import SwiftUI

struct PinPadView: View {
    @Binding var pin: String
    let pinInfoText: String
    let pinInfoHeadline: String
    let showFaceIdButton: Bool
    var biometricSystemImageName = "faceid"
    var biometricAccessibilityLabel = "Face ID"
    var onBiometricAuthentication: () -> Void = {}

    private let pinLength = 5
    private let keypadRows: [[Int]] = [
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, 9]
    ]

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                Spacer(minLength: 28)

                VStack(spacing: 18) {
                    Text(pinInfoHeadline)
                        .font(.system(size: 31, weight: .bold, design: .rounded))
                        .foregroundStyle(.primary)

                    Text(pinInfoText)
                        .font(.system(size: 17, weight: .medium, design: .rounded))
                        .foregroundStyle(.secondary)
                    Spacer()
                    HStack(spacing: 14) {
                        ForEach(0..<pinLength, id: \.self) { index in
                            Circle()
                                .fill(index < pin.count ? Color.baPrimary : Color.white)
                                .frame(width: 14, height: 14)
                                .overlay {
                                    Circle()
                                        .stroke(Color.baPrimary, lineWidth: 2)
                                }
                        }
                    }
                    .padding(.top, 10)
                    Spacer()
                }
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

                Spacer()

                VStack(spacing: 18) {
                    ForEach(keypadRows, id: \.self) { row in
                        HStack(spacing: 18) {
                            ForEach(row, id: \.self) { number in
                                keypadButton(title: "\(number)") {
                                    append(number)
                                }
                            }
                        }
                    }

                    HStack(spacing: 18) {
                        if showFaceIdButton {
                            symbolButton(
                                systemName: biometricSystemImageName,
                                accessibilityLabel: biometricAccessibilityLabel,
                                action: onBiometricAuthentication
                            )
                        } else {
                            Color.clear
                                .frame(width: 88, height: 88)
                        }

                        keypadButton(title: "0") {
                            append(0)
                        }

                        symbolButton(systemName: "delete.left", accessibilityLabel: "Löschen") {
                            deleteLast()
                        }
                    }
                }
                .padding(24)
                .shadow(color: Color.black.opacity(0.08), radius: 30, y: 12)
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
        }
    }

    private func append(_ digit: Int) {
        guard pin.count < pinLength else { return }
        pin.append(String(digit))
    }

    private func deleteLast() {
        guard !pin.isEmpty else { return }
        pin.removeLast()
    }

    private func keypadButton(title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 30, weight: .semibold, design: .rounded))
                .foregroundColor(Color.baPrimary)
                .frame(width: 88, height: 88)
        }
        .buttonStyle(.plain)
        .shadow(color: Color.black.opacity(0.08), radius: 14, y: 8)
    }

    private func symbolButton(
        systemName: String,
        accessibilityLabel: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(Color.baPrimary)
                .frame(width: 88, height: 88)
        }
        .buttonStyle(.plain)
        .shadow(color: Color.black.opacity(0.08), radius: 14, y: 8)
        .accessibilityLabel(accessibilityLabel)
    }
}

#Preview {
    PinPadPreview()
}

private struct PinPadPreview: View {
    @State private var pin = ""
    @State private var pinInfoText = "Melde dich mit deinem Pin an"
    @State private var pinHeadline = "Pin eingeben"
    @State private var showFaceIdButton: Bool = true

    var body: some View {
        PinPadView(
            pin: $pin,
            pinInfoText: pinInfoText,
            pinInfoHeadline: pinHeadline,
            showFaceIdButton: showFaceIdButton
        )
    }
}
