import SwiftUI

struct AttendanceStatusIcon: View {
    let status: HomeAttendanceStatus
    var lateMinutes: Int? = nil

    var body: some View {
        HStack(spacing: 8) {
            if status == .late, let lateMinutes {
                Text("\(lateMinutes) min")
                    .font(.caption)
            }
            Image(systemName: systemImage)
        }
        .foregroundStyle(color)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
    }

    private var systemImage: String {
        switch status {
        case .missing: "questionmark.circle"
        case .present: "checkmark.circle"
        case .late: "clock.badge.exclamationmark"
        case .absent: "xmark.circle"
        }
    }

    private var color: Color {
        switch status {
        case .missing: .primary
        case .present: Color(.baGreen)
        case .late: Color(.baOrange)
        case .absent: Color(.baRed)
        }
    }

    private var accessibilityText: String {
        if status == .late, let lateMinutes {
            return "\(lateMinutes) Minuten verspätet"
        }
        return status.rawValue
    }
}

#Preview("Alle Status") {
    VStack(alignment: .leading, spacing: 16) {
        AttendanceStatusIcon(status: .missing)
        AttendanceStatusIcon(status: .present)
        AttendanceStatusIcon(status: .late, lateMinutes: 10)
        AttendanceStatusIcon(status: .absent)
    }
    .font(.title2)
    .padding()
}
