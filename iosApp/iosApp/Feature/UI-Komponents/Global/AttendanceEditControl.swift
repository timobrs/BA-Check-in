import SwiftUI

struct AttendanceEditControl: View {
    @Binding var status: HomeAttendanceStatus

    var body: some View {
        HStack(spacing: 8) {
            if status.isPresent {
                Button {
                    status = status == .late ? .present : .late
                } label: {
                    Image(systemName: status == .late ? "checkmark.circle.fill" : "checkmark.circle")
                        .foregroundStyle(Color(.baOrange))
                        .opacity(status == .late ? 1 : 0.45)
                        .frame(minWidth: 32, minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Verspätet angekommen")
                .accessibilityValue(status == .late ? "Ja" : "Nein")
                .accessibilityAddTraits(status == .late ? [.isSelected] : [])
            }

            Toggle("Anwesend", isOn: Binding(
                get: { status.isPresent },
                set: { isPresent in
                    status = isPresent ? (status == .late ? .late : .present) : .missing
                }
            ))
            .labelsHidden()
            .tint(Color(.baGreen))
            .background(Color(.baRed), in: Capsule())
            .fixedSize()
        }
    }
}

private struct AttendanceEditControlPreview: View {
    @State private var status: HomeAttendanceStatus = .present

    var body: some View {
        VStack(spacing: 20) {
            Text("Status: \(status.rawValue)")
            AttendanceEditControl(status: $status)
        }
        .padding()
    }
}

#Preview("Interaktive Bearbeitung") {
    AttendanceEditControlPreview()
}
