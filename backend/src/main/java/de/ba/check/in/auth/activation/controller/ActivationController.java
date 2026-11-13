package de.ba.check.in.auth.activation.controller;

import de.ba.check.in.auth.activation.dto.*;
import de.ba.check.in.auth.activation.service.ActivationService;
import de.ba.check.in.auth.ratelimit.DatabaseSecurityRateLimiter;
import de.ba.check.in.auth.ratelimit.SecurityRateLimitPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/device-activations")
@RequiredArgsConstructor
@Tag(name = "Device activation")
public class ActivationController {
    private final ActivationService activationService;
    private final DatabaseSecurityRateLimiter rateLimiter;

    /** Erstellt eine Challenge. */
    @PostMapping("/challenges")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an activation challenge", description =
            "Exchanges a single-use activation code for a short-lived device challenge.")
    public ActivationChallengeResponse createChallenge(@Valid @RequestBody ActivationChallengeRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.check(SecurityRateLimitPolicy.ACTIVATION_CHALLENGE,
                servletRequest.getRemoteAddr(),
                request.activationCode());
        return activationService.createChallenge(request.activationCode());
    }

    /** Schließt die Geräteaktivierung ab. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Complete device activation", description = """
            Registers a P-256 public key after proof of possession.
            Sign the UTF-8 bytes of these newline-separated values without a trailing newline:
            `BACHECKIN_DEVICE_ACTIVATION_V1`, activation session UUID, challenge as unpadded
            Base64URL, and SHA-256 of the X.509 SubjectPublicKeyInfo as unpadded Base64URL.
            Activation itself does not issue access or refresh tokens.
            """)
    public ActivationResponse complete(@Valid @RequestBody CompleteActivationRequest request,
            HttpServletRequest servletRequest) {
        rateLimiter.check(SecurityRateLimitPolicy.ACTIVATION_COMPLETE,
                servletRequest.getRemoteAddr(),
                request.activationSessionId().toString());
        return activationService.complete(request);
    }
}
