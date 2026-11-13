//
//  DeviceActivationSuccessView.swift
//  iosApp
//
//  Created by Timo Bruns on 25.05.26.
//

import SwiftUI

struct DeviceActivationSuccessView: View {

    let result: ActivationResult

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            ZStack {
                Circle()
                    .fill(Color.baPrimary.opacity(0.12))
                    .frame(width: 110, height: 110)

                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(Color.baPrimary)
            }
            .padding(.bottom, 32)

            Text("Gerät freigeschaltet!")
                .font(.title.bold())
                .multilineTextAlignment(.center)

            Text("Dein Gerät wurde erfolgreich mit deinem Account verknüpft. Du kannst ab jetzt Check-ins durchführen.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
                .padding(.horizontal, 8)

            VStack(spacing: 12) {
                InfoRow(icon: "person.fill",        label: "Name",            value: result.displayName)
                InfoRow(icon: "person.badge.key",   label: "Rolle",           value: result.role)
            }
            .padding(20)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.top, 32)
            .padding(.horizontal)

            Spacer()
            Spacer()

            Text("Die Freischaltung ist abgeschlossen. Du kannst diese App nun für Check-ins nutzen.")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
                .padding(.bottom, 24)
        }
        .padding(20)
        .navigationTitle("Freischaltung")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
    }
}

// MARK: - Hilfskomponente: Info-Zeile

private struct InfoRow: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(Color.baPrimary)
                .frame(width: 24)

            Text(label)
                .foregroundStyle(.secondary)
                .frame(maxWidth: 110, alignment: .leading)

            Text(value)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .font(.subheadline)
    }
}

// MARK: - Vorschau

#Preview {
    NavigationStack {
        DeviceActivationSuccessView(
            result: ActivationResult(
                userId: "demo-user",
                displayName: "Demo User",
                role: "Student:in"
            )
        )
    }
}
