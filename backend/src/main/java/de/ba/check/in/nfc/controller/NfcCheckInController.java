package de.ba.check.in.nfc.controller;

import de.ba.check.in.application.dto.ApplicationDtos.NfcCheckInRequest;
import de.ba.check.in.application.dto.ApplicationDtos.NfcCheckInResult;
import de.ba.check.in.nfc.service.NfcCheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/nfc")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class NfcCheckInController {
    private final NfcCheckInService service;

    /** Verarbeitet einen NFC-Check-in. */
    @PostMapping("/check-ins")
    public NfcCheckInResult checkIn(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NfcCheckInRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID credentialId = UUID.fromString(jwt.getClaimAsString("credential_id"));
        return service.checkIn(userId, credentialId, request);
    }
}
