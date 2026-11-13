package de.ba.check.in.auth.activation.bootstrap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("dev & local-bootstrap")
public class LocalSecretaryBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LocalSecretaryBootstrapRunner.class);

    private final LocalSecretaryBootstrapService bootstrapService;
    private final UUID secretaryUserId;
    private final String activationCode;

    /** Erstellt den Startablauf. */
    public LocalSecretaryBootstrapRunner(LocalSecretaryBootstrapService bootstrapService,
            @Value("${app.security.local-bootstrap.secretary-user-id}") UUID secretaryUserId,
            @Value("${app.security.local-bootstrap.activation-code}") String activationCode) {
        this.bootstrapService = bootstrapService;
        this.secretaryUserId = secretaryUserId;
        this.activationCode = activationCode;
    }

    /** Führt den Startablauf aus. */
    @Override
    public void run(@NonNull ApplicationArguments arguments) {
        LocalBootstrapResult result = bootstrapService.provision(secretaryUserId, activationCode);
        log.warn("""
                    Local Secretary bootstrap code is ready: userId={}, activationCodeId={}, expiresAt={},
                    newlyCreated={}. Use the code supplied through BOOTSTRAP_ACTIVATION_CODE,
                    then restart without the local-bootstrap profile.
                """,
                secretaryUserId, result.activationCodeId(), result.expiresAt(), result.newlyCreated());
    }
}
