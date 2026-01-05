package com.hms.auth.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.ApiResponse;
import com.hms.auth.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()") // ✅ Any authenticated user can access
@Slf4j
public class ProfileController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(
            HttpServletRequest request) {
        
        try {
            // 🔍 Get current user from request attribute (set by JWT filter)
            User currentUser = (User) request.getAttribute("currentUser");
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("User information not found", null)
                );
            }
            
            // 📊 Build profile response based on user role
            Map<String, Object> profileData = buildProfileData(currentUser);
            
            log.info("Profile accessed by user: {} with role: {}", 
                    currentUser.getEmail(), currentUser.getRole());
            
            return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", profileData)
            );
            
        } catch (Exception e) {
            log.error("Error retrieving user profile: ", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve profile", null)
            );
        }
    }
    
    /**
     * 🏗️ Build profile data based on user role
     */
    private Map<String, Object> buildProfileData(User user) {
        Map<String, Object> profile = new HashMap<>();
        
        // 🔐 Common profile information for all users
        profile.put("userId", user.getId());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("accountStatus", user.getEnabled() ? "ACTIVE" : "INACTIVE");
        profile.put("createdAt", user.getCreatedAt());
        profile.put("lastUpdated", user.getUpdatedAt());
        profile.put("accessTime", LocalDateTime.now());
        
        // 🎭 Role-specific profile enhancements
        switch (user.getRole()) {
            case ADMIN:
                profile.put("dashboardUrl", "/admin/dashboard");
                profile.put("permissions", new String[]{"USER_MANAGEMENT", "SYSTEM_CONFIG", "REPORTS"});
                profile.put("roleDescription", "System Administrator - Full Access");
                profile.put("accessLevel", "FULL");
                break;
                
            case DOCTOR:
                profile.put("dashboardUrl", "/doctor/dashboard");
                profile.put("permissions", new String[]{"PATIENT_VIEW", "CONSULTATION", "APPOINTMENT_MANAGE"});
                profile.put("roleDescription", "Medical Doctor - Patient Care Access");
                profile.put("accessLevel", "MEDICAL");
                break;
                
            case PATIENT:
                profile.put("dashboardUrl", "/patient/dashboard");
                profile.put("permissions", new String[]{"APPOINTMENT_BOOK", "MEDICAL_HISTORY", "PROFILE_UPDATE"});
                profile.put("roleDescription", "Patient - Personal Health Access");
                profile.put("accessLevel", "PERSONAL");
                break;
                
            default:
                profile.put("dashboardUrl", "/");
                profile.put("permissions", new String[]{"BASIC_ACCESS"});
                profile.put("roleDescription", "Standard User");
                profile.put("accessLevel", "BASIC");
        }
        
        // 📈 Add session information
        profile.put("sessionInfo", Map.of(
            "loginTime", LocalDateTime.now(),
            "ipAddress", "Dynamic", // Could extract from request if needed
            "userAgent", "API_CLIENT"
        ));
        
        return profile;
    }
}
