 import SwiftUI

struct AttendanceDayRow: View {
    let day: HomeAttendanceDay
    var attendance: HomeAttendance? = nil

    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text(day.appointment.startAt, format: .dateTime.day().month().year().locale(Locale(identifier: "de_DE")))
                Text(day.appointment.title)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            if let attendance {
                AttendanceStatusIcon(status: attendance.status, lateMinutes: attendance.lateMinutes)
            } else {
                Text("\(day.presentCount)/\(day.attendances.count)")
                    .monospacedDigit()
                    .accessibilityLabel("\(day.presentCount) von \(day.attendances.count) anwesend")
            }
            Image(systemName: "chevron.right")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tertiary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }
}

#Preview("Kurs mit Zähler") {
    AttendanceDayRow(day: SecretaryPreviewData.day)
        .padding()
}

#Preview("Person verspätet") {
    AttendanceDayRow(
        day: SecretaryPreviewData.day,
        attendance: SecretaryPreviewData.attendances[1]
    )
    .padding()
}
