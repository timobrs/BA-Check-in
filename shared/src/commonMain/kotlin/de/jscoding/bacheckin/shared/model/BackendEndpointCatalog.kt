package de.jscoding.bacheckin.shared.model

object BackendEndpointCatalog {
    object Activation {
        const val CREATE_CHALLENGE = "POST /api/v1/device-activations/challenges"
        const val COMPLETE = "POST /api/v1/device-activations"
    }

    object Session {
        const val CREATE_AUTH_CHALLENGE = "POST /api/v1/device-sessions/challenges"
        const val OPEN = "POST /api/v1/device-sessions/open"
        const val REFRESH = "POST /api/v1/device-sessions/refresh"
        const val LOGOUT_DEVICE = "POST /api/v1/device-sessions/logout"
    }

    object Student {
        const val HOME = "GET /api/v1/student/home"
        const val REPORT_ABSENCE = "POST /api/v1/student/absences"
    }

    object Appointments {
        const val DOZENT_HOME = "GET /api/v1/lecturer/home"
        const val OPEN = "POST /api/v1/lecturer/appointments/{appointmentId}/open"
        const val UPDATE_ATTENDANCE =
            "PUT /api/v1/lecturer/appointments/{appointmentId}/attendances/{studentId}"
        const val VALIDATE_ATTENDANCES =
            "POST /api/v1/lecturer/appointments/{appointmentId}/attendances/validate"
        const val SELECT_PROTOKOLLANT =
            "PUT /api/v1/lecturer/appointments/{appointmentId}/protokollant"
    }

    object Verwaltung {
        const val HOME = "GET /api/v1/secretary/home"
        const val CREATE_USER_ACTIVATION_CODE =
            "POST /api/v1/secretary/users/{studentId}/device-activation-codes"
        const val REVOKE_USER_DEVICE = "POST /api/v1/secretary/users/{studentId}/device/revoke"
        const val UPDATE_ATTENDANCE =
            "PUT /api/v1/secretary/appointments/{appointmentId}/attendances/{studentId}"
        const val VALIDATE_ATTENDANCES =
            "POST /api/v1/secretary/appointments/{appointmentId}/attendances/validate"
        const val IMPORT_CALENDAR = "PUT /api/v1/secretary/calendar-imports/{sourceKey}"
        const val ASSIGN_MODULE = "PUT /api/v1/secretary/users/{studentId}/modules/{moduleId}"
        const val REMOVE_MODULE = "DELETE /api/v1/secretary/users/{studentId}/modules/{moduleId}"
        const val UPDATE_LECTURE_MODULE = "PUT /api/v1/secretary/appointments/{appointmentId}/module"
        const val REVIEW_ABSENCE = "PUT /api/v1/secretary/absences/{absenceId}"
    }

    object Nfc {
        const val CHECKIN = "POST /api/v1/student/nfc/check-ins"
    }
}
