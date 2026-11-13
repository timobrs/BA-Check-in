package de.ba.check.in.application.controller;

import de.ba.check.in.application.dto.ApplicationDtos.AppointmentOverview;
import de.ba.check.in.application.dto.ApplicationDtos.AttendanceDay;
import de.ba.check.in.application.dto.ApplicationDtos.AttendanceUpdate;
import de.ba.check.in.application.dto.ApplicationDtos.LecturerHomeData;
import de.ba.check.in.application.dto.ApplicationDtos.ProtokollantUpdate;
import de.ba.check.in.application.service.ApplicationFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lecturer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerController {
    private final ApplicationFlowService service;

    /** Lädt die Startdaten. */
    @GetMapping("/home")
    public LecturerHomeData home(@AuthenticationPrincipal Jwt jwt) {
        return service.lecturerHome(currentUserId(jwt));
    }

    /** Öffnet einen Termin. */
    @PostMapping("/appointments/{appointmentId}/open")
    public AppointmentOverview open(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        return service.openLecture(currentUserId(jwt), appointmentId);
    }

    /** Aktualisiert eine Anwesenheit. */
    @PutMapping("/appointments/{appointmentId}/attendances/{studentId}")
    public AttendanceDay updateAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId,
            @PathVariable UUID studentId,
            @RequestBody AttendanceUpdate request) {
        return service.updateAttendance(
                currentUserId(jwt),
                appointmentId,
                studentId,
                request.status(),
                false);
    }

    /** Bestätigt die Anwesenheiten. */
    @PostMapping("/appointments/{appointmentId}/attendances/validate")
    public AttendanceDay confirmAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        return service.validateAttendances(currentUserId(jwt), appointmentId, false);
    }

    /** Wählt den Protokollanten aus. */
    @PutMapping("/appointments/{appointmentId}/protokollant")
    public AppointmentOverview selectRecorder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId,
            @RequestBody ProtokollantUpdate request) {
        return service.selectProtokollant(
                currentUserId(jwt),
                appointmentId,
                request.studentId());
    }

    /** Gibt die ID des angemeldeten Nutzers zurück. */
    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
