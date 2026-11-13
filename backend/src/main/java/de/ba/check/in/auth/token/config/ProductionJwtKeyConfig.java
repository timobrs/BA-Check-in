package de.ba.check.in.auth.token.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration(proxyBeanMethods = false)
@Profile("!dev")
public class ProductionJwtKeyConfig {

    /** Lädt das JWT-Schlüsselpaar. */
    @Bean
    public KeyPair jwtKeyPair(@Value("${app.security.jwt.private-key}") Resource privateKeyResource,
            @Value("${app.security.jwt.public-key}") Resource publicKeyResource) {
        try (InputStream privateKeyInput = privateKeyResource.getInputStream();
                InputStream publicKeyInput = publicKeyResource.getInputStream()) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privateKeyInput);
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(publicKeyInput);
            if (!privateKey.getModulus().equals(publicKey.getModulus())) {
                throw new IllegalStateException("JWT private and public keys do not form a key pair");
            }
            return new KeyPair(publicKey, privateKey);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read JWT key material", exception);
        }
    }
}
