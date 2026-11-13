package de.ba.check.in.auth.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requirePositive;

@Service
public class AuthenticationDataCleanupJob {
    private static final String DELETE_EXPIRED_REFRESH_TOKENS = """
            delete from refresh_tokens
            where family_expires_at < :authenticationCutoff
            """;
    private static final String DELETE_EXPIRED_AUTH_CHALLENGES = """
            delete from auth_challenges
            where expires_at < :authenticationCutoff
            """;
    private static final String DELETE_STALE_RATE_LIMITS = """
            delete from security_rate_limits
            where updated_at < :rateLimitCutoff
              and (blocked_until is null or blocked_until <= :now)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final Duration authenticationHistoryRetention;
    private final Duration rateLimitRetention;

    /** Erstellt den Startablauf. */
    public AuthenticationDataCleanupJob(NamedParameterJdbcTemplate jdbcTemplate,
            Clock clock,
            @Value("${app.security.cleanup.authentication-history-retention:P7D}")
            Duration authenticationHistoryRetention,
            @Value("${app.security.cleanup.rate-limit-retention:P1D}") Duration rateLimitRetention) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.authenticationHistoryRetention = requirePositive(authenticationHistoryRetention,
                "app.security.cleanup.authentication-history-retention");
        this.rateLimitRetention = requirePositive(rateLimitRetention, "app.security.cleanup.rate-limit-retention");
    }

    /** Entfernt abgelaufene technische Daten. */
    @Scheduled(initialDelayString = "${app.security.cleanup.interval:PT1H}", fixedDelayString = "${app.security"
            + ".cleanup.interval:PT1H}")
    @Transactional
    public void removeExpiredTechnicalData() {
        Instant now = clock.instant();
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("authenticationCutoff",
                        Timestamp.from(now.minus(authenticationHistoryRetention)))
                .addValue("rateLimitCutoff", Timestamp.from(now.minus(rateLimitRetention)))
                .addValue("now", Timestamp.from(now));

        jdbcTemplate.update(DELETE_EXPIRED_REFRESH_TOKENS, parameters);
        jdbcTemplate.update(DELETE_EXPIRED_AUTH_CHALLENGES, parameters);
        jdbcTemplate.update(DELETE_STALE_RATE_LIMITS, parameters);
    }
}
