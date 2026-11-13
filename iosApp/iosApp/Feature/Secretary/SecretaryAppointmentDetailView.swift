import SwiftUI

struct SecretaryAppointmentDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: HomeBackendViewModel
    let courseID: String
    let appointmentID: String

    private var course: HomeCourse? {
        viewModel.courses.first { $0.id == courseID }
    }

    private var appointment: HomeAppointment? {
        course?.appointments.first { $0.id == appointmentID }
    }

    private var module: HomeModule? {
        guard let moduleID = appointment?.moduleID else { return nil }
        return course?.modules.first { $0.id == moduleID }
    }

    var body: some View {
        NavigationStack {
            Group {
                if let appointment {
                    Form {
                        appointmentDetails(appointment)
                        lecturerAssignment(appointment)
                    }
                } else {
                    ContentUnavailableView(
                        "Termin nicht gefunden",
                        systemImage: "calendar.badge.exclamationmark"
                    )
                }
            }
            .navigationTitle("Termindetails")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Schließen", systemImage: "xmark") { dismiss() }
                        .labelStyle(.iconOnly)
                }
            }
        }
        .interactiveDismissDisabled(viewModel.isLoading)
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private func appointmentDetails(_ appointment: HomeAppointment) -> some View {
        Section("Termin") {
            LabeledContent("Titel", value: appointment.title)
            LabeledContent("Zeit") {
                Text(appointmentTime(appointment))
                    .multilineTextAlignment(.trailing)
            }
            LabeledContent(
                "Ort",
                value: appointment.room.isEmpty ? "Kein Raum angegeben" : appointment.room
            )
            LabeledContent(
                "Modul",
                value: module?.name ?? "Kein Modul zugeordnet"
            )
        }
    }

    @ViewBuilder
    private func lecturerAssignment(_ appointment: HomeAppointment) -> some View {
        Section("Dozent für dieses Modul") {
            lecturerMenu(
                lecturerID: module?.lecturerID,
                enabled: module != nil && !viewModel.isLoading
            ) { lecturerID in
                guard let module else { return }
                viewModel.setModuleLecturer(moduleId: module.id, lecturerId: lecturerID)
            }
        }

        Section {
            Toggle(
                "Anderer Dozent",
                isOn: Binding(
                    get: { appointment.lecturerOverride },
                    set: { enabled in
                        viewModel.setAppointmentLecturer(
                            appointmentId: appointment.id,
                            overrideModule: enabled,
                            lecturerId: nil
                        )
                    }
                )
            )
            .disabled(!appointment.lecturerEditable || viewModel.isLoading)

            if appointment.lecturerOverride {
                lecturerMenu(
                    lecturerID: appointment.lecturerID,
                    enabled: appointment.lecturerEditable && !viewModel.isLoading
                ) { lecturerID in
                    viewModel.setAppointmentLecturer(
                        appointmentId: appointment.id,
                        overrideModule: true,
                        lecturerId: lecturerID
                    )
                }
            }
        } footer: {
            if !appointment.lecturerEditable {
                Text("Die Dozentenzuweisung kann für diesen Termin nicht mehr geändert werden.")
            } else {
                Text("Aktiviere „Anderer Dozent“, um nur für diesen Termin vom Moduldozenten abzuweichen.")
            }
        }

        if viewModel.isLoading {
            Section {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Dozentenzuweisung wird gespeichert …")
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private func lecturerMenu(
        lecturerID: String?,
        enabled: Bool,
        onSelect: @escaping (String?) -> Void
    ) -> some View {
        Menu {
            Button {
                onSelect(nil)
            } label: {
                Label(
                    "Keine Zuordnung",
                    systemImage: lecturerID == nil ? "checkmark" : "person.crop.circle.badge.minus"
                )
            }

            ForEach(viewModel.lecturers.filter { $0.active || $0.id == lecturerID }) { lecturer in
                Button {
                    onSelect(lecturer.id)
                } label: {
                    Label(
                        lecturer.displayName + (lecturer.active ? "" : " (inaktiv)"),
                        systemImage: lecturer.id == lecturerID ? "checkmark" : "person.crop.circle"
                    )
                }
            }
        } label: {
            HStack {
                Text(lecturerName(for: lecturerID))
                    .foregroundStyle(lecturerID == nil ? .secondary : .primary)
                Spacer()
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .contentShape(Rectangle())
        }
        .disabled(!enabled)
    }

    private func lecturerName(for id: String?) -> String {
        guard let id else { return "Kein Dozent zugewiesen" }
        guard let lecturer = viewModel.lecturers.first(where: { $0.id == id }) else {
            return "Unbekannter Dozent"
        }
        return lecturer.displayName + (lecturer.active ? "" : " (inaktiv)")
    }

    private func appointmentTime(_ appointment: HomeAppointment) -> String {
        let date = appointment.startAt.formatted(
            .dateTime.day().month().year().locale(Locale(identifier: "de_DE"))
        )
        let start = appointment.startAt.formatted(date: .omitted, time: .shortened)
        let end = appointment.endAt.formatted(date: .omitted, time: .shortened)
        return "\(date) · \(start)–\(end) Uhr"
    }
}

