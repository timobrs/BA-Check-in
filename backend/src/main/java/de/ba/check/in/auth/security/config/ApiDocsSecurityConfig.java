package de.ba.check.in.auth.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static de.ba.check.in.auth.config.SecurityConfigurationValidator.requireText;

@Configuration(proxyBeanMethods = false)
@Profile("api-docs")
public class ApiDocsSecurityConfig {

    /** Konfiguriert den Schutz der API-Dokumentation. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(
                        "/internal/api-docs",
                        "/internal/openapi",
                        "/internal/openapi/**",
                        "/internal/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .hasRole("API_DOCS"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /** Stellt den Passwort-Encoder für die API-Dokumentation bereit. */
    @Bean
    public PasswordEncoder apiDocsPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Konfiguriert den Zugang zur API-Dokumentation. */
    @Bean
    public UserDetailsService apiDocsUserDetailsService(@Value("${app.security.api-docs.username}") String username,
            @Value("${app.security.api-docs.password}") String password,
            PasswordEncoder apiDocsPasswordEncoder) {
        String validatedUsername = requireText(username, "app.security.api-docs.username");
        String validatedPassword = requireText(password, "app.security.api-docs.password");
        if (validatedPassword.length() < 12) {
            throw new IllegalStateException("app.security.api-docs.password must contain at least 12 characters");
        }
        return new InMemoryUserDetailsManager(User.withUsername(validatedUsername)
                .password(apiDocsPasswordEncoder.encode(validatedPassword)).roles("API_DOCS").build());
    }
}
