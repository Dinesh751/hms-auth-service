package com.hms.auth.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hms.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Health Indicator for Auth Service
 * Monitors the health of the authentication service
 */
@Component
@Slf4j
public class AuthServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private UserService userService;

    @Override
    public Health health() {
        try {
            // Check if basic service operations are working
            long userCount = userService.count();
            long activeUsers = userService.countActiveUsers();
            
            // Service is healthy if we can query the database
            if (userCount >= 0) {
                return Health.up()
                    .withDetail("service", "Auth Service")
                    .withDetail("status", "UP")
                    .withDetail("totalUsers", userCount)
                    .withDetail("activeUsers", activeUsers)
                    .withDetail("database", "Connected")
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
            } else {
                return Health.down()
                    .withDetail("service", "Auth Service")
                    .withDetail("status", "DOWN")
                    .withDetail("error", "Invalid user count")
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Auth Service Health Check Failed", e);
            return Health.down()
                .withDetail("service", "Auth Service")
                .withDetail("status", "DOWN")
                .withDetail("error", e.getMessage())
                .withDetail("errorClass", e.getClass().getSimpleName())
                .withDetail("timestamp", System.currentTimeMillis())
                .build();
        }
    }
}
