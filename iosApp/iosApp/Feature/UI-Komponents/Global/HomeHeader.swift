//
//  HomeHeader.swift
//  iosApp
//
//  Created by Timo Bruns on 04.08.26.
//
import SwiftUI

struct HomeHeader: View {
    let subtitle: String?
    var onLogout: () -> Void = {}
    
    private var greeting: String {
        switch Calendar.current.component(.hour, from: Date()) {
        case 2...10:
            return "Guten Morgen"
        case 11...18:
            return "Guten Tag"
        default:
            return "Guten Abend"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                Text(greeting)
                    .font(.largeTitle.bold())

                Spacer()

                if #available(iOS 26.0, *) {
                    NavigationLink {
                        SettingsView(onLogout: onLogout)
                    } label: {
                        Image(systemName: "gear")
                            .font(.title3)
                            .foregroundStyle(.black)
                            .frame(width: 44, height: 44)
                    }
                    .glassEffect(.regular.interactive(), in: .circle)
                } else {
                    NavigationLink {
                        SettingsView(onLogout: onLogout)
                    } label: {
                        Image(systemName: "gear")
                            .font(.title3)
                            .foregroundStyle(.black)
                            .frame(width: 44, height: 44)
                    }
                }
            }

            if let subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
    }
}
