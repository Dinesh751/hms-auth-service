package com.hms.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import com.hms.auth.service.UserService;

/**
 * User Metrics Registrar Component
 * Registers custom gauges for user metrics after application startup
 */
@Component
public class UserMetricsRegistrar {

    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private UserService userService;

    /**
     * Register custom gauges after application is fully started
     * This avoids circular dependency issues during startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerUserMetrics() {
        // Active users gauge
        Gauge.builder("auth.users.active", () -> {
            try {
                return userService.countActiveUsers();
            } catch (Exception e) {
                return 0.0;
            }
        })
        .description("Number of active users")
        .tag("service", "auth")
        .register(meterRegistry);

        // Admin users gauge
        Gauge.builder("auth.users.admin", () -> {
            try {
                return userService.countByRole("ADMIN");
            } catch (Exception e) {
                return 0.0;
            }
        })
        .description("Number of admin users")
        .tag("service", "auth")
        .tag("role", "admin")
        .register(meterRegistry);

        // Doctor users gauge
        Gauge.builder("auth.users.doctor", () -> {
            try {
                return userService.countByRole("DOCTOR");
            } catch (Exception e) {
                return 0.0;
            }
        })
        .description("Number of doctor users")
        .tag("service", "auth")
        .tag("role", "doctor")
        .register(meterRegistry);

        // Patient users gauge
        Gauge.builder("auth.users.patient", () -> {
            try {
                return userService.countByRole("PATIENT");
            } catch (Exception e) {
                return 0.0;
            }
        })
        .description("Number of patient users")
        .tag("service", "auth")
        .tag("role", "patient")
        .register(meterRegistry);
    }
}
