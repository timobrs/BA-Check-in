//
//  Avication1View.swift
//  iosApp
//
//  Created by Timo Bruns on 25.05.26.
//

import SwiftUI
import Shared

struct Avication1View: View {
    @StateObject private var avication = AvicationViewModel()
    @State private var errorMessage: String?
    @State private var isLoading: Bool = false
    @State private var showSuccess: Bool = false
    @State private var showPrivacyPolicy: Bool = false
    @State private var showTermsOfUse: Bool = false
    @FocusState private var isFocused: Bool
    private let backend: any BackendRemoteDataSource = BackendSession.shared.backend
    var keyboardType: UIKeyboardType = .default
    var onActivationCompleted: () -> Void = {}


    var body: some View {
        NavigationStack {
            GeometryReader { geometry in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Image("BaLogoBig")
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: 180)
                            .padding(.top, 24)

                        Spacer(minLength: 48)

                        VStack(alignment: .leading, spacing: 12) {
                            Text("Herzlich Willkommen")
                                .font(.title.bold())

                            Text("Gib den achtstelligen Freischaltcode ein, den du im Sekretariat erhalten hast. Es wird kein Benutzername und kein Passwort verwendet.")
                                .font(.body)
                                .foregroundStyle(.secondary)
                        }

                        VStack(alignment: .leading, spacing: 16) {
                            HStack(spacing: 8) {
                                Image(systemName: "key.horizontal")
                                    .foregroundStyle(.black)
                                    .font(.system(size: 17, weight: .regular))

                                TextField("Freischaltcode", text: $avication.registrationKey)
                                    .keyboardType(keyboardType)
                                    .autocorrectionDisabled()
                                    .textInputAutocapitalization(.never)
                                    .focused($isFocused)
                            }
                            .padding(.horizontal, 18)
                            .padding(.vertical, 14)
                            .background(
                                RoundedRectangle(cornerRadius: 28)
                                    .fill(Color.white)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 28)
                                    .stroke(isFocused ? Color.black : Color.gray.opacity(0.8), lineWidth: 1)
                            )

                            if let errorMessage {
                                HStack(spacing: 6) {
                                    Image(systemName: "exclamationmark.circle.fill")
                                    Text(errorMessage)
                                        .font(.footnote)
                                }
                                .foregroundStyle(.red)
                            }

                            Button(action: handleActivation) {
                                HStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(.circular)
                                            .tint(.white)
                                    } else {
                                        Text("Nächster Schritt")
                                        Image(systemName: "arrow.forward")
                                    }
                                }
                                .baFullWidthActionLabel()
                                .background(isFormFilled ? Color.baPrimary : Color.gray.opacity(0.4))
                                .foregroundStyle(.white)
                                .clipShape(Capsule())
                            }
                            .disabled(!isFormFilled || isLoading)
                        }
                        .padding(.top, 24)

                        Text(consentText)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .tint(Color.secondary)
                            .padding(.top, 16)
                            .environment(\.openURL, OpenURLAction { url in
                                switch url.host {
                                case "privacy-policy":
                                    showPrivacyPolicy = true
                                case "terms-of-use":
                                    showTermsOfUse = true
                                default:
                                    return .systemAction
                                }
                                return .handled
                            })
                        Spacer()
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 20)
                    .frame(minHeight: geometry.size.height, alignment: .top)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationDestination(isPresented: $showSuccess) {
                Avication2PinSelectView(
                    avication: avication,
                    onActivationCompleted: onActivationCompleted
                )
            }
            .navigationDestination(isPresented: $showPrivacyPolicy) {
                PrivacyPolicyView()
            }
            .navigationDestination(isPresented: $showTermsOfUse) {
                TermsOfUseView()
            }
        }
    }

    // MARK: - Hilfseigenschaften & Aktionen

    private var consentText: AttributedString {
        var text = try! AttributedString(
            markdown: "Mit der Freischaltung stimmst du der [Datenschutzerklärung](ba-check-in://privacy-policy) und den [Nutzungsbedingungen](ba-check-in://imprint) zu."
        )

        for run in text.runs where run.link != nil {
            text[run.range].underlineStyle = .single
        }

        return text
    }

    private var isFormFilled: Bool {
        !avication.registrationKey.isEmpty && !avication.registrationKey.trimmingCharacters(in: .whitespaces).isEmpty
    }
    private func handleActivation() {
        errorMessage = nil
        isLoading = true
        let code = avication.registrationKey.trimmingCharacters(in: .whitespacesAndNewlines)

        backend.validateActivationCode(activationCode: code) { error in
            DispatchQueue.main.async {
                defer { isLoading = false }

                if let error {
                    errorMessage = activationErrorMessage(error)
                    return
                }

                showSuccess = true
            }
        }
    }

    private func activationErrorMessage(_ error: Error) -> String {
        guard let backendError = (error as NSError).kotlinException as? BackendException else {
            return error.localizedDescription
        }

        if backendError.code.isEqual(BackendDeviceErrorCode.invalidActivationCode) {
            return "Der Freischaltungscode ist zu kurz (mind. 8 Zeichen)."
        }
        if backendError.code.isEqual(BackendDeviceErrorCode.activationCodeExpired) {
            return "Der Freischaltungscode ist abgelaufen."
        }
        if backendError.code.isEqual(BackendDeviceErrorCode.activationCodeAlreadyUsed) {
            return "Dieser Freischaltungscode wurde bereits verwendet."
        }
        return backendError.message ?? "Unbekannter Backendfehler."
    }
}

#Preview {
    Avication1View()
}
