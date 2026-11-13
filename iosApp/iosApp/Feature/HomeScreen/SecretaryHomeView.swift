//
//  SecretaryHomeView.swift
//  iosApp
//
//  Created by Timo Bruns on 17.09.26.
//

import SwiftUI

struct SecretaryHomeView: View {
    @StateObject private var viewModel: HomeBackendViewModel
    @State private var searchText = ""
    private let secretaryName: String?
    let onLogout: () -> Void

    init(
        role: String = "VERWALTUNG",
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        onLogout: @escaping () -> Void = {}
    ) {
        _viewModel = StateObject(wrappedValue: HomeBackendViewModel(role: role))
        secretaryName = bindingStore.load().displayName
        self.onLogout = onLogout
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    HomeHeader(subtitle: secretaryName, onLogout: onLogout)
                    searchBar
                        .padding(.horizontal, 16)

                    SecretaryDirectoryResults(
                        courses: viewModel.courses,
                        searchText: searchText,
                        isLoading: viewModel.isLoading
                    )
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 32)
            }
            .background(Color(.systemBackground))
            .scrollDismissesKeyboard(.interactively)
            .toolbar(.hidden, for: .navigationBar)
            .task { viewModel.load() }
            .refreshable { viewModel.load() }
            .navigationDestination(for: SecretaryRoute.self) { route in
                destination(for: route)
            }
            .alert("Aktion fehlgeschlagen", isPresented: errorBinding) {
                Button("Daten neu laden") { viewModel.load() }
                Button("OK", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "Unbekannter Fehler")
            }
        }
    }

    @ViewBuilder
    private func destination(for route: SecretaryRoute) -> some View {
        switch route {
        case .course(let courseId):
            if let course = viewModel.courses.first(where: { $0.id == courseId }) {
                SecretaryCourseDetailView(viewModel: viewModel, initialCourse: course)
            } else {
                ContentUnavailableView("Kurs nicht gefunden", systemImage: "books.vertical")
            }
        case .student(let studentId):
            if let student = viewModel.courses.lazy.flatMap(\.students).first(where: { $0.id == studentId }) {
                SecretaryStudentDetailView(viewModel: viewModel, initialStudent: student)
            } else {
                ContentUnavailableView("Nutzer nicht gefunden", systemImage: "person.crop.circle.badge.questionmark")
            }
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.clearError() } }
        )
    }

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)
            TextField("Nutzer oder Kurs eingeben", text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .accessibilityLabel("Nach Kursen oder Namen suchen")

            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color(.baPrimary))
                        .frame(minWidth: 32, minHeight: 32)
                }
                .accessibilityLabel("Suche löschen")
            }
        }
        .font(.body)
        .padding(.horizontal, 14)
        .frame(minHeight: 44)
        .background(Color(.systemGray5), in: Capsule())
    }
}

#Preview {
    SecretaryHomeView()
}
