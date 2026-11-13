//
//  CheckIn.swift
//  iosApp
//
//  Created by Timo Bruns on 17.08.26.
//

import SwiftUI

struct CheckIn: View {
    let status: CheckInStatus
    var onScan: (() -> Void)? = nil

    var body: some View {
        CheckInCard(
            content: status.cardContent,
            onScan: onScan
        )
    }
}

private struct CheckInCardContent {
    let title: String
    let message: String
    let viewfinderColor: Color
    let iconColor: Color
    let icon: String
    let showsScanningLine: Bool
}

private struct CheckInCard: View {
    let content: CheckInCardContent
    let onScan: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(content.title)
                .font(.title2.bold())
            Text(content.message)
                .font(.body.weight(.regular))

            CheckInStatusIllustration(
                viewfinderColor: content.viewfinderColor,
                iconColor: content.iconColor,
                icon: content.icon,
                showsScanningLine: content.showsScanningLine
            )
            .frame(maxHeight: .infinity)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .aspectRatio(1, contentMode: .fit)
        .cardBackground()
        .padding(.horizontal, 16)
        .contentShape(Rectangle())
        .onTapGesture {
            if content.showsScanningLine {
                onScan?()
            }
        }
        .accessibilityHint(content.showsScanningLine ? "Startet den NFC-Scan erneut" : "")
    }
}

private struct CheckInStatusIllustration: View {
    let viewfinderColor: Color
    let iconColor: Color
    let icon: String
    let showsScanningLine: Bool

    var body: some View {
        GeometryReader { geometry in
            let sideLength = min(geometry.size.width, geometry.size.height)

            ZStack {
                Image(systemName: "viewfinder")
                    .font(.system(size: sideLength * 1, weight: .light))
                    .foregroundStyle(viewfinderColor)

                if showsScanningLine {
                    ScanningLine(sideLength: sideLength)
                }

                Image(systemName: icon)
                    .font(.system(size: sideLength * 0.5, weight: .bold))
                    .foregroundStyle(iconColor)
            }
            .frame(width: sideLength, height: sideLength)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        }
    }
}

private struct ScanningLine: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let sideLength: CGFloat

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 60.0, paused: reduceMotion)) { context in
            let elapsed = context.date.timeIntervalSinceReferenceDate
            let progress = (sin(elapsed * .pi / 2.0) + 1.0) / 2.0
            Capsule()
                .fill(Color(.baPrimary))
                .frame(
                    width: sideLength * 0.9,
                    height: max(2, sideLength * 0.03)
                )
                .offset(y: reduceMotion ? 0 : (progress - 0.5) * sideLength * 0.74)
            }
    }
}

private extension CheckInStatus {
    var cardContent: CheckInCardContent {
        switch self {
        case .notCheckedIn:
            CheckInCardContent(
                title: "Check-in",
                message: "Scanne die Karte mit deinem Handy",
                viewfinderColor: .baPrimary,
                iconColor: .test,
                icon: "wave.3.right",
                showsScanningLine: true
            )
        case .checkedIn(let until):
            CheckInCardContent(title: "Eingecheckt",
                message: "Du bist bis \(until.formatted(date: .omitted, time: .shortened)) Uhr angemeldet",
                viewfinderColor: .green.opacity(0.2),
                iconColor: .green,
                icon: "checkmark",
                showsScanningLine: false
            )
        case .absent:
            CheckInCardContent(
                title: "Abwesend",
                message: "Du bist für heute als abwesend eingetragen",
                viewfinderColor: .white.opacity(0),
                iconColor: .red,
                icon: "xmark",
                showsScanningLine: false
            )
        case .waiting(let message):
            CheckInCardContent(
                title: "Check-in noch nicht verfügbar",
                message: message,
                viewfinderColor: .secondary.opacity(0.2),
                iconColor: .secondary,
                icon: "clock",
                showsScanningLine: false
            )
        case .nfcUnavailable:
            CheckInCardContent(
                title: "NFC ist nicht verfügbar",
                message: "Dieses Gerät unterstützt keinen NFC-Check-in.",
                viewfinderColor: .secondary.opacity(0.2),
                iconColor: .secondary,
                icon: "antenna.radiowaves.left.and.right.slash",
                showsScanningLine: false
            )
        case .unavailable:
            CheckInCardContent(
                title: "Check-in nicht verfügbar",
                message: "Aktuell ist kein NFC-Check-in möglich.",
                viewfinderColor: .secondary.opacity(0.2),
                iconColor: .secondary,
                icon: "antenna.radiowaves.left.and.right.slash",
                showsScanningLine: false
            )
        case .error(let message):
            CheckInCardContent(
                title: "Fehlgeschlagen",
                message: message,
                viewfinderColor: .red.opacity(0.2),
                iconColor: .red,
                icon: "xmark",
                showsScanningLine: false
            )
        }
    }
}

#Preview("Nicht eingecheckt") {
    CheckIn(status: .notCheckedIn)
}

#Preview("Eingecheckt") {
    CheckIn(
        status: .checkedIn(
            until: Date().addingTimeInterval(45 * 60)
        )
    )
}

#Preview("Abwesend") {
    CheckIn(
        status: .absent(
            until: Date().addingTimeInterval(3 * 60 * 60)
        )
    )
}

#Preview("Fehler") {
    CheckIn(
        status: .error(
            message: "Die Karte konnte nicht gelesen werden."
        )
    )
}

#Preview("NFC nicht verfügbar") {
    CheckIn(status: .nfcUnavailable)
}
