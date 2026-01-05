package com.hms.auth.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.ApiResponse;
import com.hms.auth.entity.User;
import com.hms.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // ✅ Only ADMIN role can access
@Slf4j
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard(
            HttpServletRequest request) {
        
        try {
            // 🔍 Get current admin user from request attribute (set by JWT filter)
            User currentAdmin = (User) request.getAttribute("currentUser");
            
            if (currentAdmin == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Admin information not found", null)
                );
            }
            
            // 📊 Build admin dashboard data
            Map<String, Object> dashboardData = new HashMap<>();
            
            // 👤 Admin info
            dashboardData.put("adminId", currentAdmin.getId());
            dashboardData.put("adminEmail", currentAdmin.getEmail());
            dashboardData.put("adminRole", currentAdmin.getRole().name());
            dashboardData.put("accessTime", LocalDateTime.now());
            
            // 📈 System statistics
            dashboardData.put("systemStats", Map.of(
                "totalUsers", userService.count(),
                "totalAdmins", userService.countByRole("ADMIN"),
                "totalDoctors", userService.countByRole("DOCTOR"),
                "totalPatients", userService.countByRole("PATIENT"),
                "activeUsers", userService.countActiveUsers()
            ));
            
            // 🛠️ Admin capabilities
            dashboardData.put("adminCapabilities", new String[]{
                "USER_MANAGEMENT",
                "SYSTEM_CONFIGURATION", 
                "REPORTS_AND_ANALYTICS",
                "SECURITY_MANAGEMENT",
                "AUDIT_LOGS"
            });
            
            // 🎯 Quick actions
            dashboardData.put("quickActions", Map.of(
                "manageUsers", "/api/admin/users",
                "viewReports", "/api/admin/reports", 
                "systemConfig", "/api/admin/config",
                "auditLogs", "/api/admin/audit"
            ));
            
            // ⚡ Recent activities (mock data)
            dashboardData.put("recentActivities", new Object[]{
                Map.of("action", "User Created", "target", "doctor@hospital.com", "time", LocalDateTime.now().minusHours(1)),
                Map.of("action", "User Disabled", "target", "patient@email.com", "time", LocalDateTime.now().minusHours(2)),
                Map.of("action", "System Config Updated", "target", "JWT Settings", "time", LocalDateTime.now().minusHours(3))
            });
            
            log.info("Admin dashboard accessed by: {}", currentAdmin.getEmail());
            
            return ResponseEntity.ok(
                ApiResponse.success("Admin dashboard data retrieved successfully", dashboardData)
            );
            
        } catch (Exception e) {
            log.error("Error retrieving admin dashboard: ", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve admin dashboard", null)
            );
        }
    }
}
