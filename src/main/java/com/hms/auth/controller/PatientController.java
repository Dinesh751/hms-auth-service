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
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')") // ✅ Only PATIENT role can access
@Slf4j
public class PatientController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientDashboard(
            HttpServletRequest request) {
        
        try {
            // 🔍 Get current patient user from request attribute (set by JWT filter)
            User currentPatient = (User) request.getAttribute("currentUser");
            
            if (currentPatient == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Patient information not found", null)
                );
            }
            
            // 📊 Build patient dashboard data
            Map<String, Object> dashboardData = new HashMap<>();
            
            // 👤 Patient info
            dashboardData.put("patientId", currentPatient.getId());
            dashboardData.put("patientEmail", currentPatient.getEmail());
            dashboardData.put("patientRole", currentPatient.getRole().name());
            dashboardData.put("accountStatus", currentPatient.getEnabled() ? "ACTIVE" : "INACTIVE");
            dashboardData.put("accessTime", LocalDateTime.now());
            dashboardData.put("memberSince", currentPatient.getCreatedAt());
            
            // 📈 Patient statistics
            dashboardData.put("patientStats", Map.of(
                "totalAppointments", 8, // Mock data - would come from appointment service
                "upcomingAppointments", 2,
                "completedAppointments", 6,
                "totalDoctors", userService.countByRole("DOCTOR"),
                "lastVisit", LocalDateTime.now().minusDays(15) // Mock data
            ));
            
            // 🏥 Patient capabilities
            dashboardData.put("patientCapabilities", new String[]{
                "APPOINTMENT_BOOKING",
                "MEDICAL_HISTORY_VIEW",
                "PRESCRIPTION_ACCESS",
                "TEST_RESULTS_VIEW",
                "PROFILE_MANAGEMENT"
            });
            
            // 🎯 Quick actions
            dashboardData.put("quickActions", Map.of(
                "bookAppointment", "/api/patient/appointments/book",
                "viewHistory", "/api/patient/history",
                "prescriptions", "/api/patient/prescriptions", 
                "testResults", "/api/patient/test-results",
                "updateProfile", "/api/patient/profile"
            ));
            
            // 📅 Upcoming appointments (mock data)
            dashboardData.put("upcomingAppointments", new Object[]{
                Map.of(
                    "appointmentId", "APT001",
                    "date", LocalDateTime.now().plusDays(2),
                    "doctor", "Dr. Smith",
                    "department", "Cardiology",
                    "type", "Check-up",
                    "status", "CONFIRMED"
                ),
                Map.of(
                    "appointmentId", "APT002", 
                    "date", LocalDateTime.now().plusDays(7),
                    "doctor", "Dr. Johnson",
                    "department", "General Medicine",
                    "type", "Follow-up",
                    "status", "SCHEDULED"
                )
            });
            
            // 🩺 Health summary (mock data)
            dashboardData.put("healthSummary", Map.of(
                "bloodType", "O+",
                "allergies", new String[]{"Peanuts", "Shellfish"},
                "chronicConditions", new String[]{"Hypertension"},
                "emergencyContact", Map.of(
                    "name", "Emergency Contact",
                    "relation", "Spouse",
                    "phone", "+1-555-0123"
                )
            ));
            
            // 📊 Recent activity
            dashboardData.put("recentActivity", new Object[]{
                Map.of("activity", "Appointment Booked", "date", LocalDateTime.now().minusDays(1), "details", "Dr. Smith - Cardiology"),
                Map.of("activity", "Test Results Available", "date", LocalDateTime.now().minusDays(3), "details", "Blood Test Results"),
                Map.of("activity", "Prescription Renewed", "date", LocalDateTime.now().minusDays(5), "details", "Blood pressure medication")
            });
            
            log.info("Patient dashboard accessed by: {}", currentPatient.getEmail());
            
            return ResponseEntity.ok(
                ApiResponse.success("Patient dashboard data retrieved successfully", dashboardData)
            );
            
        } catch (Exception e) {
            log.error("Error retrieving patient dashboard: ", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve patient dashboard", null)
            );
        }
    }
}
