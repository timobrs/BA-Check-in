package de.ba.check.in.auth.ratelimit;

import de.ba.check.in.api.error.RateLimitedException;
import de.ba.check.in.auth.crypto.Sha256Hasher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class DatabaseSecurityRateLimiter {
    private static final String ACQUIRE_SQL = """
            insert into security_rate_limits as limits
                (rate_key, window_started_at, attempts, blocked_until, updated_at)
            values
                (:rateKey, :now, 1, null, :now)
            on conflict (rate_key) do update set
                window_started_at = case
                    when (limits.blocked_until is not null and limits.blocked_until <= :now)
                      or limits.window_started_at <= :windowCutoff
                    then :now
                    else limits.window_started_at
                end,
                attempts = case
                    when limits.blocked_until is not null and limits.blocked_until > :now
                    then limits.attempts
                    when (limits.blocked_until is not null and limits.blocked_until <= :now)
                      or limits.window_started_at <= :windowCutoff
                    then 1
                    else limits.attempts + 1
                end,
                blocked_until = case
                    when limits.blocked_until is not null and limits.blocked_until > :now
                    then limits.blocked_until
                    when (limits.blocked_until is not null and limits.blocked_until <= :now)
                      or limits.window_started_at <= :windowCutoff
                    then null
                    when limits.attempts + 1 > :maxAttempts
                    then :newBlockedUntil
                    else null
                end,
                updated_at = :now
            returning blocked_until
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Sha256Hasher sha256Hasher;
    private final Clock clock;

    /** Erstellt eine neue Instanz. */
    public DatabaseSecurityRateLimiter(NamedParameterJdbcTemplate jdbcTemplate,
            Sha256Hasher sha256Hasher,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.sha256Hasher = sha256Hasher;
        this.clock = clock;
    }

    /** Prüft das Anfragelimit. */
    public void check(SecurityRateLimitPolicy policy, String networkIdentity, String subjectIdentity) {
        acquire(policy, "network", networkIdentity);
        acquire(policy, "subject", subjectIdentity);
    }

    /** Prüft nur den Nutzerbezug. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkSubjectOnly(SecurityRateLimitPolicy policy, String subjectIdentity) {
        acquire(policy, "subject", subjectIdentity);
    }

    /** Prüft und reserviert ein Anfragelimit. */
    private void acquire(SecurityRateLimitPolicy policy, String scope, String identity) {
        Instant now = clock.instant();
        String rateKey = rateKey(policy, scope, identity);
        MapSqlParameterSource parameters =
                new MapSqlParameterSource().addValue("rateKey", rateKey).addValue("now", Timestamp.from(now))
                        .addValue("windowCutoff", Timestamp.from(now.minus(policy.window())))
                        .addValue("maxAttempts", policy.maxAttempts())
                        .addValue("newBlockedUntil", Timestamp.from(now.plus(policy.blockDuration())));

        Instant blockedUntil = jdbcTemplate.queryForObject(ACQUIRE_SQL, parameters, (resultSet, rowNumber) -> {
            Timestamp value = resultSet.getTimestamp("blocked_until");
            return value == null ? null : value.toInstant();
        });
        if (blockedUntil != null && blockedUntil.isAfter(now)) {
            throw new RateLimitedException(blockedUntil);
        }
    }

    /** Erstellt den Schlüssel für das Anfragelimit. */
    private String rateKey(SecurityRateLimitPolicy policy, String scope, String identity) {
        String material = String.join("\n", policy.name(), scope, valueOrUnknown(identity));
        return policy.name() + ":" + scope + ":" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256Hasher.hash(material.getBytes(StandardCharsets.UTF_8)));
    }

    /** Gibt den Wert oder einen Ersatz zurück. */
    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
