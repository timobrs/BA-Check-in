import SwiftUI

struct HomeContainerView: View {
    @Environment(\.dismiss) private var dismiss
    private let bindingStore: DeviceBindingStore
    private let onLogout: (() -> Void)?

    init(
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        onLogout: (() -> Void)? = nil
    ) {
        self.bindingStore = bindingStore
        self.onLogout = onLogout
    }

    var body: some View {
        let role = bindingStore.load().userRole?.uppercased() ?? "STUDENT"
        let logoutAction = { onLogout?() ?? dismiss() }
        switch role {
        case "DOZENT":
            DozentHomeView(role: role, onLogout: logoutAction)
        case "STUDENT":
            StudentHomeView(role: role, onLogout: logoutAction)
        case "VERWALTUNG":
            SecretaryHomeView(role: role, onLogout: logoutAction)
        default:
            ContentUnavailableView(
                "Ansicht nicht verfügbar",
                systemImage: "person.crop.circle.badge.questionmark",
                description: Text("Für die Rolle \(role) ist in der iOS-App noch keine Ansicht eingerichtet.")
            )
        }
    }
}
