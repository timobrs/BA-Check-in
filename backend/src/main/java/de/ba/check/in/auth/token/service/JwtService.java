package de.ba.check.in.auth.token.service;

import de.ba.check.in.auth.token.model.IssuedAccessToken;
import de.ba.check.in.devices.domain.DeviceCredentialEntity;
import de.ba.check.in.devices.domain.DeviceEntity;
import de.ba.check.in.users.domain.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;
import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requireText;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final String audience;
    private final Duration accessTtl;

    /** Erstellt den Dienst. */
    public JwtService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience,
            @Value("${app.security.jwt.access-ttl}") Duration accessTtl) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = requireText(issuer, "app.security.jwt.issuer");
        this.audience = requireText(audience, "app.security.jwt.audience");
        this.accessTtl = requirePositive(
                accessTtl,
                "app.security.jwt.access-ttl");
    }

    /** Erstellt ein Zugriffstoken. */
    public IssuedAccessToken createAccessToken(DeviceCredentialEntity credential) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTtl);

        DeviceEntity device = credential.getDevice();
        UserEntity user = device.getUser();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("device_id", device.getId().toString())
                .claim("credential_id", credential.getId().toString())
                .claim("session_version", credential.getSessionVersion())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}
