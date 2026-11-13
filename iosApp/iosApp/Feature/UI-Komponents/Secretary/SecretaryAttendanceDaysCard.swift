import SwiftUI

struct SecretaryAttendanceDaysCard: View {
    let days: [HomeAttendanceDay]
    var studentId: String? = nil
    let onSelect: (HomeAttendanceDay) -> Void

    private var visibleDays: [HomeAttendanceDay] {
        var seenIds = Set<String>()
        return days.filter { day in
            (studentId == nil || day.attendances.contains { $0.id == studentId })
                && seenIds.insert(day.id).inserted
        }.sorted { $0.appointment.startAt > $1.appointment.startAt }
    }

    var body: some View {
        let days = visibleDays

        VStack(alignment: .leading, spacing: 8) {
            Text("Tage")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            if days.isEmpty {
                ContentUnavailableView(
                    "Keine Anwesenheiten vorhanden",
                    systemImage: studentId == nil ? "person.2.badge.checkmark" : "calendar"
                )
            } else {
                SecretaryListCard(items: days) { day in
                    Button {
                        onSelect(day)
                    } label: {
                        AttendanceDayRow(day: day, attendance: day.attendances.first { $0.id == studentId })
                    }
                    .accessibilityHint(studentId == nil
                        ? "Öffnet die Anwesenheiten für diesen Tag"
                        : "Öffnet die Bearbeitung der Anwesenheit für diesen Tag")
                }
            }
        }
    }
}

#Preview("Anwesenheitstage eines Kurses") {
    SecretaryAttendanceDaysCard(days: [SecretaryPreviewData.day, SecretaryPreviewData.secondDay]) { _ in }
        .padding()
}

#Preview("Anwesenheitstage einer Person") {
    SecretaryAttendanceDaysCard(
        days: [SecretaryPreviewData.day, SecretaryPreviewData.secondDay],
        studentId: "123460",
        onSelect: { _ in }
    )
    .padding()
}
