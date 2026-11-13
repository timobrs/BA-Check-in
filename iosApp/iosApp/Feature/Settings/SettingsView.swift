//
//  SettingsView.swift
//  iosApp
//
//  Created by Timo Bruns on 31.07.26.
//

import SwiftUI

struct SettingsView: View {
    @StateObject private var viewModel = BiometricOptInViewModel()
    @State private var logoutErrorMessage: String?
    @State private var showLogoutAlert: Bool = false
    @State private var isLoggingOut = false

    var onLogout: () -> Void = {}
    private let logoutService = LocalLogoutService()

    init(onLogout: @escaping () -> Void = {}) {
        self.onLogout = onLogout
    }

    var body: some View {
        NavigationView{
            ScrollView {
                VStack(alignment: .leading, spacing: 28) {
                    VStack(alignment: .leading, spacing: 8) {
                        settingsSectionTitle("Account")

                        VStack(spacing: 0) {
                            Toggle(
                                "Anmelden mit biometrischen Daten",
                                isOn: Binding(
                                    get: { viewModel.isBiometricUnlockEnabled },
                                    set: viewModel.setBiometricsEnabled
                                )
                            )
                            .padding(.vertical, 14)
                            .disabled(viewModel.isAuthenticating)
                            .tint(.baPrimary)

                            Divider()

                            Button {
                                showLogoutAlert = true
                            } label: {
                                HStack {
                                    Text("Abmelden")
                                    Spacer()
                                    Image(systemName: "info.circle")
                                        .foregroundStyle(.baPrimary)
                                    Image(systemName: "chevron.right")
                                        .font(.footnote.weight(.semibold))
                                        .foregroundStyle(.tertiary)
                                }
                                .padding(.vertical, 14)
                            }
                            .buttonStyle(.plain)
                            .disabled(isLoggingOut)
                            .alert("Möchtest du dich abmelden?", isPresented: $showLogoutAlert) {
                                Button("Abbrechen", role: .cancel) {}
                                Button("Abmelden", role: .destructive, action: handleLogout)
                            } message: {
                                Text("Durch das Abmelden werden lokale Tokens und Geräteschlüssel gelöscht. Für die erneute Nutzung brauchst du einen neuen Freischaltungscode aus dem Sekretariat.")
                            }
                        }
                        .padding(.horizontal, 16)
                        .settingsCardBackground()
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        settingsSectionTitle("Rechtliches")

                        VStack(spacing: 0) {
                            NavigationLink {
                                ImprintView()
                            } label: {
                                settingsNavigationLabel("Impressum")
                            }
                            .buttonStyle(.plain)

                            Divider()

                            NavigationLink {
                                PrivacyPolicyView()
                            } label: {
                                settingsNavigationLabel("Datenschutzerklärung")
                            }
                            .buttonStyle(.plain)

                            Divider()

                            NavigationLink {
                                TermsOfUseView()
                            } label: {
                                settingsNavigationLabel("Nutzungsbedingungen")
                            }
                            .buttonStyle(.plain)

                            Divider()

                            HStack{
                                Text("Version")
                                Spacer()
                                Text(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "–")
                                    .foregroundStyle(.gray)
                                    .opacity(0.7)
                            }
                            .padding(.vertical, 14)
                        }
                        .padding(.horizontal, 16)
                        .settingsCardBackground()
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 24)
            }
            .background(Color(.systemBackground))
            .safeAreaInset(edge: .bottom, spacing: 0) {
                Image("BaLogoBig")
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity, maxHeight: 180)
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .background(Color(.systemBackground))
                    .accessibilityLabel("BA Logo")
            }
            .navigationTitle("Einstellungen")
            .navigationBarTitleDisplayMode(.inline)
            .alert(
                "Biometrische Anmeldung nicht verfügbar",
                isPresented: Binding(
                    get: { viewModel.errorMessage != nil },
                    set: { if !$0 { viewModel.errorMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "")
            }
            .alert(
                "Abmelden fehlgeschlagen",
                isPresented: Binding(
                    get: { logoutErrorMessage != nil },
                    set: { if !$0 { logoutErrorMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(logoutErrorMessage ?? "")
            }
        }
    }

    private func settingsSectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.secondary)
            .padding(.horizontal, 16)
    }

    private func settingsNavigationLabel(_ title: String) -> some View {
        HStack {
            Text(title)
                .foregroundStyle(.primary)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }

    private func handleLogout() {
        isLoggingOut = true
        logoutService.logout { result in
            isLoggingOut = false
            switch result {
            case .success:
                onLogout()
            case .failure(let error):
                logoutErrorMessage = error.localizedDescription
            }
        }
    }
}

private extension View {
    func settingsCardBackground() -> some View {
        let shape = RoundedRectangle(cornerRadius: 24, style: .continuous)

        return background(Color(.systemBackground), in: shape)
            .overlay {
                shape.stroke(.primary.opacity(0.08), lineWidth: 0.5)
            }
            .shadow(color: .black.opacity(0.06), radius: 10, x: 0, y: 4)
    }
}

#Preview {
    SettingsView()
}
