package de.ba.check.in.auth.activation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivationChallengeRequest(
        @NotBlank(message = "Activation code must not be blank")
        @Size(
                min = 16,
                max = 19,
                message = "Activation code must contain 16 code characters"
        )
        @Pattern(
                regexp = "(?i)^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ-]+$",
                message = "Activation code contains invalid characters"
        )
        String activationCode
) {
}
