import SwiftUI

struct SecretaryListCard<Item: Identifiable, Row: View>: View {
    let items: [Item]
    var cornerRadius: CGFloat = 28
    @ViewBuilder let row: (Item) -> Row

    var body: some View {
        VStack(spacing: 0) {
            ForEach(items) { item in
                row(item)
                if item.id != items.last?.id {
                    Divider()
                }
            }
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 12)
        .cardBackground(cornerRadius: cornerRadius)
        .shadow(color: .black.opacity(0.08), radius: 16, y: 10)
    }
}

#Preview("Listenkarte") {
    SecretaryListCard(items: SecretaryPreviewData.students) { student in
        HStack {
            Text(student.displayName)
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .padding(.vertical, 14)
    }
    .padding()
}
