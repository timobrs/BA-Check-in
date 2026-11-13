package de.ba.check.in.auth.token.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DevelopmentJwtKeyConfig {

    /** Lädt das JWT-Schlüsselpaar. */
    @Bean
    public KeyPair jwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate development JWT key pair", exception);
        }
    }
}
