import SwiftUI

struct MyAppointmentsCard: View {
    let appointments: [HomeAppointment]
    let title: String
    let selectedAppointmentId: String?
    let onSelect: ((HomeAppointment) -> Void)?
    
    @State private var selectedDay: AppointmentDay = .today

    init(
        appointments: [HomeAppointment],
        title: String = "Meine Termine",
        selectedAppointmentId: String? = nil,
        onSelect: ((HomeAppointment) -> Void)? = nil
    ) {
        self.appointments = appointments
        self.title = title
        self.selectedAppointmentId = selectedAppointmentId
        self.onSelect = onSelect
    }
    
    func backgroundColor(for index: Int) -> Color {
        index.isMultiple(of: 2)
            ? Color(.baAppointmentColorOne)
            : Color(.baAppointmentColorTwo)
    }
    
    private var filteredAppointments: [HomeAppointment] {
        let calendar = Calendar.autoupdatingCurrent
        let dayOffset = selectedDay == .today ? 0 : 1
        
        guard let selectedDate = calendar.date(
            byAdding: .day,
            value: dayOffset,
            to: Date()
        ) else {
            return []
        }
        
        return appointments
            .filter { calendar.isDate($0.startAt, inSameDayAs: selectedDate) }
            .sorted { $0.startAt < $1.startAt }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.title2.bold())
            
            Picker("Tag", selection: $selectedDay) {
                ForEach(AppointmentDay.allCases) { day in
                    Text(day.title).tag(day)
                }
            }
            .pickerStyle(.segmented)
            .tint(Color(.baPrimary))
            .foregroundStyle(Color(.baPrimary))
            
            if filteredAppointments.isEmpty {
                Text("Keine Termine für \(selectedDay.emptyStateTitle).")
                    .foregroundStyle(.secondary)
            }
            
            ForEach(Array(filteredAppointments.enumerated()), id: \.element.id) { index, appointment in
                if let onSelect {
                    Button {
                        onSelect(appointment)
                    } label: {
                        appointmentRow(appointment, index: index)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint("Öffnet die Anwesenheitsverwaltung")
                } else {
                    appointmentRow(appointment, index: index)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .aspectRatio(1, contentMode: .fit)
        .cardBackground()
        .padding(.horizontal, 16)
    }

    private func appointmentRow(_ appointment: HomeAppointment, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(appointment.title).font(.headline)
                Spacer()
                if appointment.id == selectedAppointmentId {
                    Image(systemName: "checkmark.circle.fill")
                        .accessibilityLabel("Ausgewählt")
                } else if onSelect != nil {
                    Image(systemName: "chevron.right")
                        .accessibilityHidden(true)
                }
            }
            HStack {
                Image(systemName: "mappin.and.ellipse.circle")
                Text(appointment.room)
            }
            Text("\(appointment.startAt.formatted(date: .omitted, time: .shortened))–\(appointment.endAt.formatted(date: .omitted, time: .shortened)) Uhr")
                .italic()
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 14)
                .fill(backgroundColor(for: index))
        }
        .contentShape(Rectangle())
    }
}

private enum AppointmentDay: CaseIterable, Identifiable {
    case today
    case tomorrow
    
    var id: Self { self }
    
    var title: String {
        switch self {
        case .today: "Heute"
        case .tomorrow: "Morgen"
        }
    }
    
    var emptyStateTitle: String {
        switch self {
        case .today: "heute"
        case .tomorrow: "morgen"
        }
    }
}
