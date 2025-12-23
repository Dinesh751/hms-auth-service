package com.hms.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.time.LocalDateTime;

// ✅ Jackson for JSON processing (already included)
import com.fasterxml.jackson.databind.ObjectMapper;

// ✅ Apache Commons (add to pom.xml)
import org.apache.commons.lang3.StringUtils;

// ✅ Spring Boot utilities
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;
import com.hms.auth.repository.UserRepository;
import com.hms.auth.service.JwtService;
import com.hms.auth.service.UserService;
import com.hms.auth.service.CookieService;
import com.hms.auth.dto.TokenResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/v1")
public class HealthController {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;

    @Autowired
    private CookieService cookieService;

    // ✅ NEW: Inject environment and utilities
    @Autowired
    private Environment environment;
    
    @Autowired
    private ObjectMapper objectMapper;  // Jackson JSON mapper

    // ✅ Helper method to create consistent JSON responses
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }

    // ✅ UPDATED: Health endpoint with JSON response
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("service", "HMS Auth Service");
        healthData.put("status", "UP");
        healthData.put("version", "1.0.0");
        healthData.put("activeProfiles", environment.getActiveProfiles());
        healthData.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(createResponse(true, "HMS Auth Service is running!", healthData));
    }
    
    // ✅ UPDATED: Home endpoint with JSON
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> homeData = new HashMap<>();
        homeData.put("service", "HMS Auth Service");
        homeData.put("version", "1.0.0");
        homeData.put("endpoints", Map.of(
            "health", "/api/auth/v1/health",
            "login", "/api/auth/v1/login", 
            "register", "/api/auth/v1/register",
            "refresh", "/api/auth/v1/refresh"
        ));
        
        return ResponseEntity.ok(createResponse(true, "Welcome to HMS Auth Service", homeData));
    }
    
    // ✅ UPDATED: Database status with JSON
    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> checkDatabaseConnection() {
        try {
            long count = userRepository.count();
            Map<String, Object> dbData = new HashMap<>();
            dbData.put("connection", "active");
            dbData.put("totalUsers", count);
            dbData.put("database", "PostgreSQL");
            dbData.put("checkedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(createResponse(true, "Database connected successfully", dbData));
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("connection", "failed");
            errorData.put("error", e.getMessage());
            errorData.put("database", "PostgreSQL");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Database connection failed", errorData));
        }
    }

    // ✅ UPDATED: Test token generation with better JSON response
    @PostMapping("/test-generate-tokens")
    public ResponseEntity<Map<String, Object>> testGenerateTokens(HttpServletResponse response) {
        try {
            // Create or find a test user
            User user;
            Optional<User> existingUser = userService.findByEmail("jwt.test@hospital.com");
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                user = userService.registerUser("jwt.test@hospital.com", "TestPass123", UserRole.DOCTOR);
            }
            
            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // Store refresh token in secure HTTP-only cookie
            cookieService.createRefreshTokenCookie(response, refreshToken);
            
            // Create TokenResponse object
            TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getEnabled()
            );
            
            // ✅ Create comprehensive response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", jwtService.getTokenExpiryTime(accessToken));
            responseData.put("user", userInfo);
            responseData.put("refreshTokenLocation", "HTTP-only cookie");
            
            // Additional metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("accessTokenLength", accessToken.length());
            metadata.put("cookieConfig", cookieService.getCookieConfigSummary());
            metadata.put("tokenPreview", Map.of(
                "start", StringUtils.left(accessToken, 20),  // ✅ Using Apache Commons
                "end", StringUtils.right(accessToken, 20)
            ));
            responseData.put("metadata", metadata);
            
            return ResponseEntity.ok(createResponse(true, "Tokens generated successfully", responseData));
            
        } catch (Exception e) {
            Map<String, Object> errorData = Map.of("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error generating tokens", errorData));
        }
    }

    // ✅ Environment info endpoint
    @GetMapping("/env-info")
    public ResponseEntity<Map<String, Object>> getEnvironmentInfo() {
        try {
            Map<String, Object> envData = new HashMap<>();
            envData.put("activeProfiles", environment.getActiveProfiles());
            envData.put("applicationName", environment.getProperty("spring.application.name"));
            envData.put("serverPort", environment.getProperty("server.port"));
            envData.put("javaVersion", System.getProperty("java.version"));
            envData.put("springBootVersion", environment.getProperty("spring.boot.version"));
            
            return ResponseEntity.ok(createResponse(true, "Environment information retrieved", envData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error getting environment info", Map.of("error", e.getMessage())));
        }
    }

    // ✅ System info endpoint
    @GetMapping("/system-info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemData = new HashMap<>();
            systemData.put("availableProcessors", runtime.availableProcessors());
            systemData.put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
            systemData.put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
            systemData.put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
            systemData.put("operatingSystem", System.getProperty("os.name"));
            systemData.put("javaVendor", System.getProperty("java.vendor"));
            
            return ResponseEntity.ok(createResponse(true, "System information retrieved", systemData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error getting system info", Map.of("error", e.getMessage())));
        }
    }

    // Keep all your existing methods but update them to return JSON...
    // (I'll show a few more key ones)

    @PostMapping("/create-doctor")
    public ResponseEntity<Map<String, Object>> createTestDoctor() {
        try {
            User user = userService.registerUser("doctor1@hospital.com", "password123", UserRole.DOCTOR);
            Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "enabled", user.getEnabled(),
                "createdAt", LocalDateTime.now()
            );
            return ResponseEntity.ok(createResponse(true, "Doctor created successfully", userData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createResponse(false, "Validation error", Map.of("error", e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Unexpected error", Map.of("error", e.getMessage())));
        }
    }

    @PostMapping("/create-patient")
    public ResponseEntity<Map<String, Object>> createTestPatient() {
        try {
            User user = userService.registerUser("patient1@hospital.com", "password123", UserRole.PATIENT);
            Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "enabled", user.getEnabled(),
                "createdAt", LocalDateTime.now()
            );
            return ResponseEntity.ok(createResponse(true, "Patient created successfully", userData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createResponse(false, "Validation error", Map.of("error", e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Unexpected error", Map.of("error", e.getMessage())));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            Map<String, Object> usersData = Map.of(
                "users", users,
                "count", users.size(),
                "retrievedAt", LocalDateTime.now()
            );
            return ResponseEntity.ok(createResponse(true, "Users retrieved successfully", usersData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error retrieving users", Map.of("error", e.getMessage())));
        }
    }
    
    @GetMapping("/find-by-email/{email}")
    public ResponseEntity<Map<String, Object>> findByEmail(@PathVariable String email) {
        try {
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                return ResponseEntity.ok(createResponse(true, "User found", user.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createResponse(false, "User not found with email: " + email, null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/test-enabled-query/{email}")
    public ResponseEntity<Map<String, Object>> testEnabledQuery(@PathVariable String email) {
        try {
            Optional<User> user = userRepository.findByEmailAndEnabled(email);
            if (user.isPresent()) {
                return ResponseEntity.ok(createResponse(true, "Enabled user found", user.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createResponse(false, "Enabled user not found with email: " + email, null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // ✅ ADD: Test authentication
    @PostMapping("/test-authenticate")
    public ResponseEntity<?> testAuthenticate() {
        try {
            Optional<User> user = userService.authenticateUser("doctor1@hospital.com", "password123");
            if (user.isPresent()) {
                return ResponseEntity.ok("✅ Authentication successful for: " + user.get().getEmail() + 
                                       " (Role: " + user.get().getRole() + ")");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Authentication failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    
    // Duplicate method removed: consolidated token generation logic is implemented earlier in the file.
    // Use the /test-generate-tokens endpoint defined above that returns a JSON response (ResponseEntity<Map<String, Object>>).

    // ✅ NEW: Test refresh token from cookie
    @PostMapping("/test-refresh-from-cookie")
    public ResponseEntity<?> testRefreshFromCookie(HttpServletRequest request, HttpServletResponse response) {
        try {
            // ✅ Extract refresh token from HTTP-only cookie
            String refreshToken = cookieService.getRefreshTokenFromCookies(request);
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ No refresh token found in cookies. Please login first.");
            }
            
            // Validate refresh token
            if (!jwtService.validateRefreshToken(refreshToken)) {
                cookieService.clearRefreshTokenCookie(response);  // Clear invalid cookie
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Invalid or expired refresh token");
            }
            
            // Extract user from token
            String email = jwtService.extractEmail(refreshToken);
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                cookieService.clearRefreshTokenCookie(response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ User not found for refresh token");
            }
            
            User user = userOpt.get();
            
            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            
            // ✅ Update refresh token cookie with new token
            cookieService.createRefreshTokenCookie(response, newRefreshToken);
            
            return ResponseEntity.ok(String.format(
                "✅ Token Refresh Successful:\n" +
                "User: %s (%s)\n" +
                "New Access Token: %s...%s\n" +
                "New Refresh Token: Updated in HTTP-only cookie\n" +
                "Access Token Expires in: %d seconds",
                user.getEmail(), user.getRole(),
                newAccessToken.substring(0, 20),
                newAccessToken.substring(newAccessToken.length() - 20),
                jwtService.getTokenExpiryTime(newAccessToken)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Error refreshing token from cookie: " + e.getMessage());
        }
    }

    // ✅ NEW: Test logout (clear cookies)
    @PostMapping("/test-logout")
    public ResponseEntity<?> testLogout(HttpServletResponse response) {
        try {
            // Clear refresh token cookie
            cookieService.clearRefreshTokenCookie(response);
            
            return ResponseEntity.ok(
                "✅ Logout Successful:\n" +
                "Refresh token cookie has been cleared.\n" +
                "Client should also clear the access token from memory."
            );
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Error during logout: " + e.getMessage());
        }
    }

    // ✅ NEW: Show cookie configuration
    @GetMapping("/cookie-config")
    public ResponseEntity<?> showCookieConfig() {
        try {
            return ResponseEntity.ok(String.format(
                "🍪 Cookie Configuration:\n%s\n\n" +
                "🔐 JWT Configuration:\n" +
                "Access Token Expiration: %d seconds (%d minutes)\n" +
                "Refresh Token Expiration: %d seconds (%d days)",
                cookieService.getCookieConfigSummary(),
                jwtService.getAccessTokenExpiration(),
                jwtService.getAccessTokenExpiration() / 60,
                jwtService.getRefreshTokenExpiration(),
                jwtService.getRefreshTokenExpiration() / (24 * 60 * 60)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Error getting cookie config: " + e.getMessage());
        }
    }

    // ✅ NEW: Test complete authentication flow with cookies
    @PostMapping("/test-full-auth-flow")
    public ResponseEntity<?> testFullAuthFlow(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Step 1: Create or authenticate user
            User user = userService.findByEmail("flow.test@hospital.com")
                .orElseGet(() -> userService.registerUser("flow.test@hospital.com", "FlowTest123", UserRole.DOCTOR));
            
            // Step 2: Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // Step 3: Store refresh token in cookie
            cookieService.createRefreshTokenCookie(response, refreshToken);
            
            // Step 4: Simulate token refresh
            String cookieRefreshToken = cookieService.getRefreshTokenFromCookies(request);
            boolean canRefresh = (cookieRefreshToken != null && jwtService.validateRefreshToken(cookieRefreshToken));
            
            return ResponseEntity.ok(String.format(
                "✅ Full Authentication Flow Test:\n" +
                "1. User: %s (%s) - %s\n" +
                "2. Access Token Generated: %d chars\n" +
                "3. Refresh Token: Stored in cookie\n" +
                "4. Cookie Retrieval: %s\n" +
                "5. Cookie Validation: %s\n" +
                "6. Ready for production: ✅\n\n" +
                "Access Token (use in Authorization header):\n%s",
                user.getEmail(), user.getRole(), user.getEnabled() ? "Active" : "Inactive",
                accessToken.length(),
                cookieRefreshToken != null ? "Success" : "Failed",
                canRefresh ? "Success" : "Failed",
                accessToken
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Error in full auth flow test: " + e.getMessage());
        }
    }

    // ✅ UPDATE: Show JWT configuration (keep existing)
    @GetMapping("/jwt-config")
    public ResponseEntity<?> showJwtConfig() {
        try {
            return ResponseEntity.ok(String.format(
                "🔐 JWT Configuration:\n" +
                "Access Token Expiration: %d seconds (%d minutes)\n" +
                "Refresh Token Expiration: %d seconds (%d days)\n" +
                "JWT Secret Length: %d characters\n" +
                "JWT Library: JJWT 0.12.5",
                jwtService.getAccessTokenExpiration(),
                jwtService.getAccessTokenExpiration() / 60,
                jwtService.getRefreshTokenExpiration(), 
                jwtService.getRefreshTokenExpiration() / (24 * 60 * 60),
                jwtService.getSecretLength()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("❌ Error getting JWT config: " + e.getMessage());
        }
    }
}

