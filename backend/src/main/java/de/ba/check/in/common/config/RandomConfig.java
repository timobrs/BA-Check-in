package de.ba.check.in.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
public class RandomConfig {

    /** Stellt eine sichere Zufallsquelle bereit. */
    @Bean
    public SecureRandom secureRandom(){
        return new SecureRandom();
    }
}
