package de.ba.check.in.application.controller;

import de.ba.check.in.application.dto.ApplicationDtos.AbsenceRequest;
import de.ba.check.in.application.dto.ApplicationDtos.StudentHomeData;
import de.ba.check.in.application.service.ApplicationFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {
    private final ApplicationFlowService service;

    /** Lädt die Startdaten. */
    @GetMapping("/home")
    public StudentHomeData home(@AuthenticationPrincipal Jwt jwt) {
        return service.studentHome(currentUserId(jwt));
    }

    /** Meldet eine Abwesenheit. */
    @PostMapping("/absences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AbsenceRequest request) {
        service.reportAbsence(
                currentUserId(jwt),
                request.fromEpochMillis(),
                request.untilEpochMillis());
    }

    /** Gibt die ID des angemeldeten Nutzers zurück. */
    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
