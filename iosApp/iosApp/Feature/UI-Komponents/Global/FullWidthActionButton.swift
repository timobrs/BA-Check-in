import SwiftUI

extension View {
    func baFullWidthActionLabel() -> some View {
        frame(maxWidth: .infinity)
            .padding(.vertical, 10)
    }
}
