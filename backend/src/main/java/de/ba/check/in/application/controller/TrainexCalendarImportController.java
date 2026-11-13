package de.ba.check.in.application.controller;

import de.ba.check.in.application.dto.ApplicationDtos.CalendarImportResult;
import de.ba.check.in.application.service.TrainexCalendarImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/secretary/calendar-imports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SECRETARY')")
public class TrainexCalendarImportController {

    private final TrainexCalendarImportService service;

    /** Importiert Kalenderdaten. */
    @PutMapping(
            path = "/{sourceKey:[a-fA-F0-9]{64}}",
            consumes = "text/calendar",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CalendarImportResult importCalendar(
            @PathVariable String sourceKey,
            @RequestBody String rawIcs
    ) {
        String normalizedSourceKey = sourceKey.toLowerCase(Locale.ROOT);
        return service.importCalendar(normalizedSourceKey, rawIcs);
    }
}
