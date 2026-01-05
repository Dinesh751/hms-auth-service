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
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')") // ✅ Only DOCTOR role can access
@Slf4j
public class DoctorController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorDashboard(
            HttpServletRequest request) {
        
        try {
            // 🔍 Get current doctor user from request attribute (set by JWT filter)
            User currentDoctor = (User) request.getAttribute("currentUser");
            
            if (currentDoctor == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Doctor information not found", null)
                );
            }
            
            // 📊 Build doctor dashboard data
            Map<String, Object> dashboardData = new HashMap<>();
            
            // 👨‍⚕️ Doctor info
            dashboardData.put("doctorId", currentDoctor.getId());
            dashboardData.put("doctorEmail", currentDoctor.getEmail());
            dashboardData.put("doctorRole", currentDoctor.getRole().name());
            dashboardData.put("accessTime", LocalDateTime.now());
            
            // 📈 Medical statistics
            dashboardData.put("medicalStats", Map.of(
                "totalPatients", userService.countByRole("PATIENT"),
                "totalDoctors", userService.countByRole("DOCTOR"),
                "activePatients", userService.countActiveUsers(), // Active users as proxy
                "newPatientsToday", 5, // Mock data
                "appointmentsToday", 12 // Mock data
            ));
            
            // 🩺 Doctor capabilities
            dashboardData.put("doctorCapabilities", new String[]{
                "PATIENT_CONSULTATION",
                "MEDICAL_RECORDS_ACCESS",
                "APPOINTMENT_MANAGEMENT", 
                "PRESCRIPTION_WRITING",
                "MEDICAL_HISTORY_REVIEW"
            });
            
            // 🎯 Quick actions
            dashboardData.put("quickActions", Map.of(
                "viewPatients", "/api/doctor/patients",
                "todayAppointments", "/api/doctor/appointments/today",
                "emergencyCases", "/api/doctor/emergency",
                "patientRecords", "/api/doctor/records"
            ));
            
            // 📅 Today's schedule (mock data)
            dashboardData.put("todaySchedule", new Object[]{
                Map.of("time", "09:00 AM", "patient", "John Doe", "type", "Consultation", "status", "Scheduled"),
                Map.of("time", "10:30 AM", "patient", "Jane Smith", "type", "Follow-up", "status", "In Progress"), 
                Map.of("time", "02:00 PM", "patient", "Bob Johnson", "type", "Check-up", "status", "Scheduled"),
                Map.of("time", "03:30 PM", "patient", "Alice Brown", "type", "Emergency", "status", "Urgent")
            });
            
            // 🏥 Department info
            dashboardData.put("departmentInfo", Map.of(
                "department", "General Medicine", // Could be stored in user profile
                "floor", "3rd Floor",
                "room", "Room 302",
                "extension", "Ext 1234"
            ));
            
            log.info("Doctor dashboard accessed by: {}", currentDoctor.getEmail());
            
            return ResponseEntity.ok(
                ApiResponse.success("Doctor dashboard data retrieved successfully", dashboardData)
            );
            
        } catch (Exception e) {
            log.error("Error retrieving doctor dashboard: ", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve doctor dashboard", null)
            );
        }
    }
}
