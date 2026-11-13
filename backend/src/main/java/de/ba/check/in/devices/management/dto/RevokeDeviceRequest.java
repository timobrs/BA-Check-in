package de.ba.check.in.devices.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeDeviceRequest(
        @NotBlank(message = "A revoke reason is required")
        @Size(
                max = 500,
                message = "Revoke reason must not exceed 500 characters"
        )
        String reason
) {
}
