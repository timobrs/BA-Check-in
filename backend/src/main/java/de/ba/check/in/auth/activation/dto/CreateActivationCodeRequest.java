package de.ba.check.in.auth.activation.dto;

import de.ba.check.in.auth.activation.domain.ActivationMode;
public record CreateActivationCodeRequest(
        ActivationMode mode
) {
}
