package de.ba.check.in.auth.activation.bootstrap;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production-bootstrap")
public class ProductionSecretaryBootstrapRunner implements ApplicationRunner {
    private final ProductionSecretaryBootstrapService service;
    private final ConfigurableApplicationContext context;
    private final String firstName;
    private final String lastName;
    private final String email;

    /** Erstellt den Startablauf. */
    public ProductionSecretaryBootstrapRunner(ProductionSecretaryBootstrapService service,
            ConfigurableApplicationContext context,
            @Value("${app.security.production-bootstrap.first-name}") String firstName,
            @Value("${app.security.production-bootstrap.last-name}") String lastName,
            @Value("${app.security.production-bootstrap.email}") String email) {
        this.service = service;
        this.context = context;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /** Führt den Startablauf aus. */
    @Override
    public void run(@NonNull ApplicationArguments arguments) {
        ProductionBootstrapResult result = service.provision(firstName, lastName, email);
        System.out.printf("PRODUCTION_BOOTSTRAP_SECRETARY_USER_ID=%s%n", result.secretaryUserId());
        System.out.printf("PRODUCTION_BOOTSTRAP_ACTIVATION_CODE=%s%n", result.activationCode());
        System.out.printf("PRODUCTION_BOOTSTRAP_EXPIRES_AT=%s%n", result.expiresAt());
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
