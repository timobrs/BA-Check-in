package de.ba.check.in.application.controller;

import de.ba.check.in.application.dto.ApplicationDtos.AbsenceReviewRequest;
import de.ba.check.in.application.dto.ApplicationDtos.AttendanceDay;
import de.ba.check.in.application.dto.ApplicationDtos.AttendanceUpdate;
import de.ba.check.in.application.dto.ApplicationDtos.LectureModuleUpdate;
import de.ba.check.in.application.dto.ApplicationDtos.SecretaryHomeData;
import de.ba.check.in.application.dto.ApplicationDtos.IgnoreUpdate;
import de.ba.check.in.application.dto.ApplicationDtos.LecturerUpdate;
import de.ba.check.in.application.dto.ApplicationDtos.LectureLecturerUpdate;
import de.ba.check.in.application.service.ApplicationFlowService;
import de.ba.check.in.devices.management.service.DeviceManagementService;
import de.ba.check.in.modules.service.ModuleManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secretary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SECRETARY')")
public class SecretaryApplicationController {
    private final ApplicationFlowService service;
    private final DeviceManagementService deviceManagementService;
    private final ModuleManagementService moduleManagementService;

    /** Lädt die Startdaten. */
    @GetMapping("/home")
    public SecretaryHomeData home() {
        return service.secretaryHome();
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
                true);
    }

    /** Bestätigt die Anwesenheiten. */
    @PostMapping("/appointments/{appointmentId}/attendances/validate")
    public AttendanceDay confirmAttendance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID appointmentId) {
        return service.validateAttendances(currentUserId(jwt), appointmentId, true);
    }

    /** Sperrt ein Gerät. */
    @PostMapping("/users/{userId}/device/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeDevice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @RequestBody(required = false) Object unusedBody) {
        deviceManagementService.revokeActiveForUser(
                userId,
                currentUserId(jwt),
                "Revoked by secretary");
    }

    /** Ordnet ein Modul zu. */
    @PutMapping("/users/{userId}/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignModule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @PathVariable UUID moduleId) {
        moduleManagementService.assign(currentUserId(jwt), userId, moduleId);
    }

    /** Entfernt ein Modul. */
    @DeleteMapping("/users/{userId}/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeModule(@PathVariable UUID userId, @PathVariable UUID moduleId) {
        moduleManagementService.remove(userId, moduleId);
    }

    /** Aktualisiert das Modul eines Termins. */
    @PutMapping("/appointments/{appointmentId}/module")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLectureModule(
            @PathVariable UUID appointmentId,
            @RequestBody LectureModuleUpdate request) {
        moduleManagementService.updateLectureModule(appointmentId, request.moduleId());
    }

    /** Aktualisiert den Ignorierstatus eines Termins. */
    @PutMapping("/appointments/{appointmentId}/ignored")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAppointmentIgnored(
            @PathVariable UUID appointmentId,
            @RequestBody IgnoreUpdate request) {
        service.updateLectureIgnored(appointmentId, request.ignored());
    }

    /** Aktualisiert den Ignorierstatus eines Moduls. */
    @PutMapping("/modules/{moduleId}/ignored")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateModuleIgnored(
            @PathVariable UUID moduleId,
            @RequestBody IgnoreUpdate request) {
        service.updateModuleIgnored(moduleId, request.ignored());
    }

    /** Aktualisiert den Dozenten eines Moduls. */
    @PutMapping("/modules/{moduleId}/lecturer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateModuleLecturer(@PathVariable UUID moduleId, @RequestBody LecturerUpdate request) {
        moduleManagementService.updateModuleLecturer(moduleId, request.lecturerId());
    }

    /** Aktualisiert den Dozenten eines Termins. */
    @PutMapping("/appointments/{appointmentId}/lecturer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLectureLecturer(@PathVariable UUID appointmentId, @RequestBody LectureLecturerUpdate request) {
        moduleManagementService.updateLectureLecturer(appointmentId, request.overrideModule(), request.lecturerId());
    }

    /** Prüft die Abwesenheitsmeldung. */
    @PutMapping("/absences/{absenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reviewAbsence(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID absenceId,
            @RequestBody AbsenceReviewRequest request) {
        service.reviewAbsence(currentUserId(jwt), absenceId, request.status());
    }

    /** Gibt die ID des angemeldeten Nutzers zurück. */
    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
