package com.hms.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Custom Metrics Configuration for HMS Auth Service
 * Defines custom metrics for monitoring authentication service
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for successful login attempts
     */
    @Bean
    public Counter loginSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.login.success")
                .description("Number of successful login attempts")
                .tag("service", "auth")
                .register(meterRegistry);
    }

    /**
     * Counter for failed login attempts
     */
    @Bean
    public Counter loginFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.login.failure")
                .description("Number of failed login attempts")
                .tag("service", "auth")
                .register(meterRegistry);
    }

    /**
     * Counter for user registrations
     */
    @Bean
    public Counter userRegistrationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.registration.total")
                .description("Number of user registrations")
                .tag("service", "auth")
                .register(meterRegistry);
    }

    /**
     * Counter for JWT token generations
     */
    @Bean
    public Counter jwtTokenCounter(MeterRegistry meterRegistry) {
        return Counter.builder("auth.jwt.generated")
                .description("Number of JWT tokens generated")
                .tag("service", "auth")
                .register(meterRegistry);
    }

    /**
     * Timer for authentication requests
     */
    @Bean
    public Timer authenticationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("auth.request.duration")
                .description("Authentication request processing time")
                .tag("service", "auth")
                .register(meterRegistry);
    }
}
