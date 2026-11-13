package de.ba.check.in.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** Stellt die Uhr bereit. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
