//
//  CardBackground.swift
//  iosApp
//

import SwiftUI

extension View {
    @ViewBuilder
    func cardBackground(cornerRadius: CGFloat = 28) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius)

        if #available(iOS 26.0, *) {
            background(.background.opacity(0.25), in: shape)
                .glassEffect(.regular, in: shape)
                .overlay {
                    shape.stroke(.primary.opacity(0.05), lineWidth: 1)
                }
        } else {
            background(.background.secondary, in: shape)
                .overlay {
                    shape.stroke(.primary.opacity(0.12), lineWidth: 1)
                }
        }
    }
}
