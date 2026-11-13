import SwiftUI

struct SecretaryCourseDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: HomeBackendViewModel
    let initialCourse: HomeCourse
    @State private var selectedTab: CourseTab = .management
    @State private var selectedAttendanceDay: HomeAttendanceDay?
    @State private var selectedAppointment: AppointmentSelection?

    private var course: HomeCourse {
        viewModel.courses.first { $0.id == initialCourse.id } ?? initialCourse
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                tabPicker
                    .accessibilityLabel("Kursansicht")

                switch selectedTab {
                case .management:
                    studentsSection
                case .attendance:
                    SecretaryAttendanceDaysCard(days: course.attendanceDays) {
                        selectedAttendanceDay = $0
                    }
                case .appointments:
                    appointmentsSection
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
        .background(Color(.systemBackground))
        .navigationTitle(course.name)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden()
        .toolbar(.visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Zurück", systemImage: "chevron.left") { dismiss() }
                    .labelStyle(.iconOnly)
                    .tint(.primary)
            }
        }
        .sheet(item: $selectedAttendanceDay) { day in
            SecretaryAttendanceDayView(viewModel: viewModel, scope: .courseDay(day.id))
        }
        .sheet(item: $selectedAppointment) { selection in
            SecretaryAppointmentDetailView(
                viewModel: viewModel,
                courseID: course.id,
                appointmentID: selection.id
            )
        }
    }

    private var tabPicker: some View {
        Picker("Kursansicht", selection: $selectedTab) {
            ForEach(CourseTab.allCases) { tab in
                Text(tab.rawValue).tag(tab)
            }
        }
        .pickerStyle(.segmented)
        .tint(Color(.baPrimary))
        .foregroundStyle(Color(.baPrimary))
    }

    @ViewBuilder
    private var studentsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle("Alle Nutzer")

            if course.students.isEmpty {
                ContentUnavailableView("Keine Nutzer vorhanden", systemImage: "person.2")
            } else {
                SecretaryListCard(items: course.students) { student in
                    NavigationLink(value: SecretaryRoute.student(student.id)) {
                        HStack(spacing: 12) {
                            Text(student.displayName)
                                .foregroundStyle(.primary)
                            Spacer(minLength: 8)
                            Image(systemName: "chevron.right")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.tertiary)
                                .accessibilityHidden(true)
                        }
                        .padding(.vertical, 16)
                        .frame(minHeight: 52)
                        .contentShape(Rectangle())
                    }
                }
            }
        }
    }

    private var upcomingAppointments: [HomeAppointment] {
        let calendar = Calendar.autoupdatingCurrent
        let start = calendar.startOfDay(for: Date())
        guard let end = calendar.date(byAdding: .day, value: 7, to: start) else { return [] }
        return course.appointments
            .filter { $0.startAt >= start && $0.startAt < end }
            .sorted { $0.startAt < $1.startAt }
    }

    private var appointmentsSection: some View {
        VStack(alignment: .leading, spacing: 24) {
            Button(action: viewModel.load) {
                HStack(spacing: 6) {
                    if viewModel.isLoading {
                        ProgressView().tint(.white)
                    } else {
                        Image(systemName: "arrow.triangle.2.circlepath")
                    }
                    Text(viewModel.isLoading ? "Termine werden geladen …" : "Terminekalender aktualisieren")
                }
                .font(.subheadline)
                .foregroundStyle(.white)
                .baFullWidthActionLabel()
                .padding(.horizontal, 8)
                .background(Color(.baPrimary), in: Capsule())
            }
            .buttonStyle(.plain)
            .disabled(viewModel.isLoading)

            VStack(alignment: .leading, spacing: 10) {
                sectionTitle("Termine in den nächsten 7 Tagen")

                if upcomingAppointments.isEmpty {
                    ContentUnavailableView(
                        "Keine Termine",
                        systemImage: "calendar",
                        description: Text("Für diesen Kurs stehen in den nächsten sieben Tagen keine Termine an.")
                    )
                } else {
                    VStack(spacing: 12) {
                        ForEach(upcomingAppointments) { appointment in
                            appointmentCard(appointment)
                        }
                    }
                }
            }
        }
    }

    private func appointmentCard(_ appointment: HomeAppointment) -> some View {
        Button {
            selectedAppointment = AppointmentSelection(id: appointment.id)
        } label: {
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(appointment.title)
                        .font(.headline)
                    Spacer(minLength: 8)
                    if appointment.lecturerID == nil {
                        Image(systemName: "person.crop.circle.badge.exclamationmark")
                            .foregroundStyle(.red)
                            .accessibilityLabel("Dozentenzuweisung fehlt")
                    }
                }
                if let moduleName = moduleName(for: appointment) {
                    Label(moduleName, systemImage: "books.vertical")
                }
                Label(
                    appointment.room.isEmpty ? "Kein Raum angegeben" : appointment.room,
                    systemImage: "mappin.and.ellipse.circle"
                )
                Text("\(appointment.startAt.formatted(.dateTime.day().month().year().locale(Locale(identifier: "de_DE")))): \(appointment.startAt.formatted(date: .omitted, time: .shortened))–\(appointment.endAt.formatted(date: .omitted, time: .shortened)) Uhr")
                    .italic()
            }
            .font(.subheadline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(Color(.baAppointmentColorOne), in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .accessibilityHint("Öffnet die Termindetails und Dozentenzuweisung")
    }

    private func moduleName(for appointment: HomeAppointment) -> String? {
        guard let moduleID = appointment.moduleID else { return nil }
        return course.modules.first { $0.id == moduleID }?.name
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.secondary)
            .padding(.horizontal, 16)
    }

    private enum CourseTab: String, CaseIterable, Identifiable {
        case management = "Verwaltung"
        case attendance = "Anwesenheiten"
        case appointments = "Termine"

        var id: Self { self }
    }
}

private struct AppointmentSelection: Identifiable {
    let id: String
}

private struct SecretaryCourseDetailPreview: View {
    @StateObject private var viewModel = HomeBackendViewModel(role: "VERWALTUNG")

    var body: some View {
        NavigationStack {
            SecretaryCourseDetailView(viewModel: viewModel, initialCourse: SecretaryPreviewData.course)
        }
        .task { viewModel.load() }
    }
}

#Preview("Kursdetails") {
    SecretaryCourseDetailPreview()
}
