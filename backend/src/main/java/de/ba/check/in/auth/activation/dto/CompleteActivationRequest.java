package de.ba.check.in.auth.activation.dto;

import de.ba.check.in.devices.domain.Platform;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record CompleteActivationRequest(
        @NotNull(message = "Activation session ID is required")
        UUID activationSessionId,

        @NotNull(message = "Platform is required")
        Platform platform,

        @NotBlank(message = "Device name is required")
        @Size(max = 255, message = "Device name must not exceed 255 characters")
        String deviceName,

        @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
        String manufacturer,

        @Size(max = 100, message = "Model must not exceed 100 characters")
        String model,

        @Size(max = 100, message = "OS version must not exceed 100 characters")
        String osVersion,

        @Size(max = 100, message = "App version must not exceed 100 characters")
        String appVersion,

        @NotBlank(message = "Public key is required")
        @Size(max = 4096, message = "Public key is too large")
        String publicKeyBase64,

        @NotBlank(message = "Challenge signature is required")
        @Size(max = 1024, message = "Challenge signature is too large")
        String challengeSignatureBase64
) {}
