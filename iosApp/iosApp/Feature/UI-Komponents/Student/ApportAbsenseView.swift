//
//  ApportAbsenseView.swift
//  iosApp
//
//  Created by Timo Bruns on 03.09.26.
//

import SwiftUI

struct ApportAbsenseView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var from = Date()
    @State private var until = Calendar.current.date(
        byAdding: .minute,
        value: 3 * 60 + 15,
        to: Date()
    ) ?? Date()
    @State private var isAllDay = false
    let onReport: (Date, Date) -> Void

    private var reportInterval: (from: Date, until: Date) {
        guard isAllDay else {
            return (from, until)
        }

        let calendar = Calendar.autoupdatingCurrent
        let startOfDay = calendar.startOfDay(for: from)
        let startOfNextDay = calendar.date(
            byAdding: .day,
            value: 1,
            to: startOfDay
        ) ?? startOfDay.addingTimeInterval(24 * 60 * 60)

        return (startOfDay, startOfNextDay)
    }

    private var isValidInterval: Bool {
        reportInterval.from < reportInterval.until
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Form {
                    Toggle("Ganztägig", isOn: $isAllDay)
                        .tint(.baPrimary)

                    if isAllDay {
                        DatePicker(
                            "Tag",
                            selection: $from,
                            displayedComponents: .date
                        )
                    } else {
                        DatePicker("Von", selection: $from)
                        DatePicker("Bis", selection: $until, in: from...)
                    }
                }

                Button {
                    onReport(reportInterval.from, reportInterval.until)
                } label: {
                    Text("Abwesenheit melden")
                        .baFullWidthActionLabel()
                        .background(isValidInterval ? Color.baPrimary : Color.gray.opacity(0.4))
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .disabled(!isValidInterval)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
            }
            .navigationTitle("Abwesenheit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel("Abbrechen")
                }
            }
        }
        .presentationDetents([.medium])
    }
}

#Preview {
    ApportAbsenseView { _, _ in }
}
