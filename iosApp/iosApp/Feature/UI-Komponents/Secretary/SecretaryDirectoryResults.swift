import SwiftUI

struct SecretaryDirectoryResults: View {
    let courses: [HomeCourse]
    let searchText: String
    let isLoading: Bool

    private var query: String {
        searchText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var results: [Result] {
        let matchingCourses = courses
            .filter { query.isEmpty || $0.name.localizedStandardContains(query) }
            .map(Result.course)
        guard !query.isEmpty else { return matchingCourses }

        var seenIds = Set<String>()
        let matchingStudents = courses.flatMap(\.students)
            .filter { $0.displayName.localizedStandardContains(query) && seenIds.insert($0.id).inserted }
            .sorted { $0.displayName.localizedStandardCompare($1.displayName) == .orderedAscending }
            .map(Result.student)
        return matchingCourses + matchingStudents
    }

    var body: some View {
        let results = results

        VStack(alignment: .leading, spacing: 12) {
            Text(query.isEmpty ? "Alle Kurse" : "Suchergebnisse")
                .font(.headline)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)

            if isLoading && courses.isEmpty {
                ProgressView("Kurse werden geladen …")
                    .frame(maxWidth: .infinity, minHeight: 160)
            } else if results.isEmpty {
                ContentUnavailableView(
                    query.isEmpty ? "Keine Kurse vorhanden" : "Keine Ergebnisse",
                    systemImage: query.isEmpty ? "books.vertical" : "magnifyingglass",
                    description: Text(query.isEmpty
                        ? "Hier erscheinen die verfügbaren Kurse."
                        : "Für „\(query)“ wurden keine Kurse oder Namen gefunden.")
                )
            } else {
                SecretaryListCard(items: results, cornerRadius: 32) { result in
                    switch result {
                    case .course(let course):
                        NavigationLink(value: SecretaryRoute.course(course.id)) {
                            directoryRow(title: course.name)
                        }
                    case .student(let student):
                        NavigationLink(value: SecretaryRoute.student(student.id)) {
                            directoryRow(title: student.displayName, subtitle: courseNames(for: student))
                        }
                    }
                }
            }
        }
    }

    private func courseNames(for student: HomeStudent) -> String {
        courses.filter { $0.students.contains { $0.id == student.id } }
            .map(\.name).joined(separator: ", ")
    }

    private func directoryRow(title: String, subtitle: String? = nil) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .foregroundStyle(.primary)
                if let subtitle {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.right")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tertiary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 14)
        .frame(minHeight: 52)
        .contentShape(Rectangle())
    }

    private enum Result: Identifiable {
        case course(HomeCourse)
        case student(HomeStudent)

        var id: String {
            switch self {
            case .course(let course): "course:\(course.id)"
            case .student(let student): "student:\(student.id)"
            }
        }
    }
}

#Preview("Alle Kurse") {
    NavigationStack {
        SecretaryDirectoryResults(
            courses: SecretaryPreviewData.courses,
            searchText: "",
            isLoading: false
        )
        .padding()
    }
}

#Preview("Suche nach Person") {
    NavigationStack {
        SecretaryDirectoryResults(
            courses: SecretaryPreviewData.courses,
            searchText: "Noah",
            isLoading: false
        )
        .padding()
    }
}
