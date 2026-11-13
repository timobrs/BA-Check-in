import SwiftUI

struct DozentHomeView: View {
    @StateObject private var viewModel: HomeBackendViewModel
    @State private var attendanceSheetRoute: AttendanceSheetRoute?
    private let lecturerName: String?
    let onLogout: () -> Void

    init(
        role: String = "DOZENT",
        bindingStore: DeviceBindingStore = DeviceBindingStore(),
        onLogout: @escaping () -> Void = {}
    ) {
        _viewModel = StateObject(wrappedValue: HomeBackendViewModel(role: role))
        lecturerName = bindingStore.load().displayName
        self.onLogout = onLogout
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    HomeHeader(subtitle: lecturerName ?? "Dozentenansicht", onLogout: onLogout)
                    activeAppointmentCard
                    MyAppointmentsCard(
                        appointments: viewModel.appointments,
                        title: "Meine Vorlesungen",
                        selectedAppointmentId: viewModel.activeAppointmentId,
                        onSelect: { viewModel.openAppointment(id: $0.id) }
                    )
                }
                .padding(.bottom, 24)
            }
            .toolbar(.hidden, for: .navigationBar)
            .refreshable { viewModel.load() }
            .overlay {
                if viewModel.isLoading && viewModel.appointments.isEmpty {
                    ProgressView()
                }
            }
            .task { viewModel.load() }
            .sheet(item: $attendanceSheetRoute) { route in
                AttendanceEditorView(viewModel: viewModel, route: route)
            }
            .alert("Backend-Fehler", isPresented: errorBinding) {
                Button("Erneut versuchen") { viewModel.load() }
                Button("OK", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "Unbekannter Fehler")
            }
        }
        .tint(Color(.baPrimary))
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.clearError() } }
        )
    }

    @ViewBuilder
    private var activeAppointmentCard: some View {
        if let appointment = viewModel.activeAppointment {
            VStack(alignment: .leading, spacing: 12) {
                Button {
                    attendanceSheetRoute = .overview
                } label: {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Aktuelle Vorlesung")
                            .font(.title2.bold())
                        appointmentDetails(appointment)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityHint("Öffnet die Anwesenheitsübersicht")

                Divider()
                if viewModel.attendancesValidated {
                    Text("Du hast aktuell nichts zu tun 😊")
                } else {
                    HStack {
                        Text("Protokollant")
                        Spacer()
                        Menu {
                            ForEach(viewModel.attendances) { candidate in
                                Button {
                                    viewModel.selectProtokollant(studentId: candidate.id)
                                } label: {
                                    if candidate.displayName == appointment.protokollant {
                                        Label(candidate.displayName, systemImage: "checkmark")
                                    } else {
                                        Text(candidate.displayName)
                                    }
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Text(protokollantLabel(for: appointment))
                                Image(systemName: "chevron.up.chevron.down")
                                    .font(.caption2)
                            }
                            .foregroundStyle(.baPrimary)
                        }
                        .tint(.baPrimary)
                        .disabled(viewModel.isLoading || viewModel.attendances.isEmpty)
                    }

                    Divider()

                    VStack(alignment: .leading) {
                        (Text("Es sind ")
                            + Text("\(presentCount)").bold()
                            + Text(" von ")
                            + Text("\(viewModel.attendances.count)").bold()
                            + Text(" Studenten anwesend"))

                        Button { attendanceSheetRoute = .confirmationEditor } label: {
                            Text("Bestätigen")
                                .baFullWidthActionLabel()
                        }
                        .background(Color(.baGreen))
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                        .disabled(!viewModel.hasSelectedProtokollant)
                        .opacity(viewModel.hasSelectedProtokollant ? 1 : 0.5)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
            .cardBackground()
            .padding(.horizontal, 16)
        } else {
            ContentUnavailableView(
                "Keine aktive Vorlesung",
                systemImage: "calendar.badge.clock",
                description: Text("Wähle unten einen Termin aus.")
            )
            .frame(minHeight: 220)
        }
    }

    private func appointmentDetails(_ appointment: HomeAppointment) -> some View {
        Group {
            Text(appointment.title)
                .font(.headline)
            Text(appointment.course)
                .foregroundStyle(.secondary)
            HStack {
                Text("\(appointment.startAt.formatted(date: .omitted, time: .shortened))–\(appointment.endAt.formatted(date: .omitted, time: .shortened)) Uhr")
                Spacer()
                HStack {
                    Image(systemName: "mappin.and.ellipse.circle")
                    Text(appointment.room)
                }
            }
        }
    }

    private var presentCount: Int {
        viewModel.attendances.filter {
            $0.status == .present || $0.status == .late
        }.count
    }

    private func protokollantLabel(for appointment: HomeAppointment) -> String {
        viewModel.hasSelectedProtokollant ? appointment.protokollant : "Auswählen"
    }

}

#Preview { DozentHomeView() }
