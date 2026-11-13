import SwiftUI

struct AbsenseCard: View {
    let absences: [HomeAbsence]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Gemeldete Abwesenheiten").font(.title2.bold())
            if absences.isEmpty {
                Text("Noch keine Abwesenheiten gemeldet.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(absences) { absence in
                    VStack(alignment: .leading, spacing: 6) {
                        Text("\(absence.fromAt.formatted(date: .long, time:.omitted))")
                            .font(.headline)
                        Text("\(absence.fromAt.formatted(date: .omitted, time: .shortened)) - \(absence.untilAt.formatted(date: .omitted, time: .shortened))")
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background{
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color(.baRed))
                    }
                    if absence.id != absences.last?.id {
                        Divider()
                    }
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .aspectRatio(1, contentMode: .fit)
        .cardBackground()
        .padding(.horizontal, 16)
    }
}
