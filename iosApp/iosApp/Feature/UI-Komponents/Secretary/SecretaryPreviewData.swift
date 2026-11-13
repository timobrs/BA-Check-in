import Foundation

enum SecretaryPreviewData {
    static let students = [
        HomeStudent(
            id: "123456",
            displayName: "Max Mustermann",
            activationCode: "STUDENT26091234",
            activatedSince: Date().addingTimeInterval(-86_400)
        ),
        HomeStudent(id: "123460", displayName: "Noah Schneider", activatedSince: Date().addingTimeInterval(-172_800)),
        HomeStudent(id: "123465", displayName: "Tim Fischer")
    ]

    static let appointment = HomeAppointment(
        id: "11111111-1111-1111-1111-111111111111",
        title: "Einführung in die Informatik",
        room: "203",
        course: "WS-26-IV",
        protokollant: "Max Mustermann",
        startAt: Date().addingTimeInterval(-1_800),
        endAt: Date().addingTimeInterval(5_400)
    )

    static let upcomingAppointment = HomeAppointment(
        id: "22222222-2222-2222-2222-222222222222",
        title: "Datenbanken & Architekturen",
        room: "102",
        course: "WS-26-IV",
        protokollant: "Noah Schneider",
        startAt: Date().addingTimeInterval(86_400),
        endAt: Date().addingTimeInterval(93_600)
    )

    static let attendances = [
        HomeAttendance(id: "123456", displayName: "Max Mustermann", status: .present),
        HomeAttendance(id: "123460", displayName: "Noah Schneider", status: .late, lateMinutes: 10),
        HomeAttendance(id: "123465", displayName: "Tim Fischer", status: .absent)
    ]

    static let day = HomeAttendanceDay(appointment: appointment, attendances: attendances)

    static let secondDay = HomeAttendanceDay(
        appointment: upcomingAppointment,
        attendances: [
            HomeAttendance(id: "123456", displayName: "Max Mustermann", status: .missing),
            HomeAttendance(id: "123460", displayName: "Noah Schneider", status: .present),
            HomeAttendance(id: "123465", displayName: "Tim Fischer", status: .absent)
        ]
    )

    static let course = HomeCourse(
        id: "WS-26-IV",
        name: "WS-26-IV",
        students: students,
        appointments: [appointment, upcomingAppointment],
        attendanceDays: [day, secondDay]
    )

    static let secondCourse = HomeCourse(
        id: "WS-24-II",
        name: "WS-24-II",
        students: Array(students.prefix(2)),
        appointments: [upcomingAppointment],
        attendanceDays: [secondDay]
    )

    static let courses = [course, secondCourse]
}
