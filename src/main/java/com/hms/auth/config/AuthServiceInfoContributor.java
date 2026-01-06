package com.hms.auth.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hms.auth.service.UserService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Info Contributor for HMS Auth Service
 * Provides service-specific information for monitoring and management
 */
@Component
public class AuthServiceInfoContributor implements InfoContributor {

    @Autowired
    private UserService userService;

    @Override
    public void contribute(Info.Builder builder) {
        try {
            Map<String, Object> serviceInfo = new HashMap<>();
            
            // Service Details
            serviceInfo.put("name", "HMS Authentication Service");
            serviceInfo.put("description", "JWT-based authentication and authorization service for Hospital Management System");
            serviceInfo.put("version", "1.0.0");
            serviceInfo.put("type", "microservice");
            serviceInfo.put("architecture", "Spring Boot 3.x with JWT");
            
            // Service Statistics
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.count());
            stats.put("totalAdmins", userService.countByRole("ADMIN"));
            stats.put("totalDoctors", userService.countByRole("DOCTOR"));
            stats.put("totalPatients", userService.countByRole("PATIENT"));
            stats.put("activeUsers", userService.countActiveUsers());
            serviceInfo.put("statistics", stats);
            
            // Service Capabilities
            serviceInfo.put("features", new String[]{
                "JWT Authentication",
                "Role-based Authorization", 
                "User Registration",
                "Password Management",
                "Multi-role Support (Admin, Doctor, Patient)",
                "Health Monitoring",
                "Metrics Collection"
            });
            
            // API Endpoints
            Map<String, String> endpoints = new HashMap<>();
            endpoints.put("authentication", "/api/auth/v1/login");
            endpoints.put("registration", "/api/auth/v1/register");
            endpoints.put("adminDashboard", "/api/admin/dashboard");
            endpoints.put("doctorDashboard", "/api/doctor/dashboard");
            endpoints.put("patientDashboard", "/api/patient/dashboard");
            endpoints.put("userProfile", "/api/profile");
            endpoints.put("health", "/actuator/health");
            endpoints.put("metrics", "/actuator/metrics");
            serviceInfo.put("endpoints", endpoints);
            
            // Runtime Information
            Map<String, Object> runtime = new HashMap<>();
            runtime.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            runtime.put("uptime", System.currentTimeMillis());
            runtime.put("javaVersion", System.getProperty("java.version"));
            runtime.put("springProfile", System.getProperty("spring.profiles.active", "dev"));
            serviceInfo.put("runtime", runtime);
            
            // Security Information
            Map<String, Object> security = new HashMap<>();
            security.put("authenticationMethod", "JWT Bearer Token");
            security.put("tokenExpiration", "15 minutes (access), 7 days (refresh)");
            security.put("encryption", "HS512");
            security.put("roles", new String[]{"ADMIN", "DOCTOR", "PATIENT"});
            serviceInfo.put("security", security);
            
            builder.withDetail("authService", serviceInfo);
            
        } catch (Exception e) {
            builder.withDetail("authServiceError", "Unable to collect service information: " + e.getMessage());
        }
    }
}
