package com.hms.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.auth.config.SecurityConfig;
import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.RegisterRequest;
import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;
import com.hms.auth.service.CookieService;
import com.hms.auth.service.JwtService;
import com.hms.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CookieService cookieService;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String TEST_ACCESS_TOKEN = "test.access.token";
    private final String TEST_REFRESH_TOKEN = "test.refresh.token";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.PATIENT);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testHealthCheck_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/auth/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    @Test
    void testRegister_ValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("StrongPass123");
        registerRequest.setRole("PATIENT");

        when(userService.registerUser(anyString(), anyString(), any(UserRole.class)))
                .thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(TEST_REFRESH_TOKEN);
        when(jwtService.getTokenExpiryTime(TEST_ACCESS_TOKEN)).thenReturn(3600L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value(TEST_ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));

        verify(cookieService).createRefreshTokenCookie(any(HttpServletResponse.class), eq(TEST_REFRESH_TOKEN));
    }

    @Test
    void testRegister_InvalidRole_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("StrongPass123");
        registerRequest.setRole("INVALID_ROLE");

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid role INVALID_ROLE"));

        verify(userService, never()).registerUser(anyString(), anyString(), any(UserRole.class));
    }

    @Test
    void testRegister_UserAlreadyExists_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("StrongPass123");
        registerRequest.setRole("PATIENT");

        when(userService.registerUser(anyString(), anyString(), any(UserRole.class)))
                .thenThrow(new IllegalArgumentException("User with email already exists: test@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.error").value("User with email already exists: test@example.com"));
    }

    @Test
    void testRegister_InternalError_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("StrongPass123");
        registerRequest.setRole("PATIENT");

        when(userService.registerUser(anyString(), anyString(), any(UserRole.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.error").value("Database connection failed"));
    }

    @Test
    void testLogin_ValidCredentials_ShouldReturnOk() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(userService.authenticateUser(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(testUser)).thenReturn(TEST_REFRESH_TOKEN);
        when(jwtService.getTokenExpiryTime(TEST_ACCESS_TOKEN)).thenReturn(3600L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value(TEST_ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("PATIENT"));

        verify(cookieService).createRefreshTokenCookie(any(HttpServletResponse.class), eq(TEST_REFRESH_TOKEN));
    }

    @Test
    void testLogin_InvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpassword");

        when(userService.authenticateUser(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication failed"))
                .andExpect(jsonPath("$.error").value("Invalid email or password"));

        verify(cookieService, never()).createRefreshTokenCookie(any(HttpServletResponse.class), anyString());
    }

    @Test
    void testLogin_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(userService.authenticateUser(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Login failed"))
                .andExpect(jsonPath("$.error").value("Database connection failed"));
    }

    @Test
    void testRefreshToken_ValidToken_ShouldReturnOk() throws Exception {
        // Arrange
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
        when(jwtService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TEST_REFRESH_TOKEN)).thenReturn("test@example.com");
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new.refresh.token");
        when(jwtService.getTokenExpiryTime(TEST_ACCESS_TOKEN)).thenReturn(3600L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refresh successful"))
                .andExpect(jsonPath("$.data.accessToken").value(TEST_ACCESS_TOKEN))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        verify(cookieService).createRefreshTokenCookie(any(HttpServletResponse.class), eq("new.refresh.token"));
    }

    @Test
    void testRefreshToken_NoToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token refresh failed"))
                .andExpect(jsonPath("$.error").value("No refresh token found"));
    }

    @Test
    void testRefreshToken_InvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenReturn("invalid.token");
        when(jwtService.validateRefreshToken("invalid.token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token refresh failed"))
                .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"));

        verify(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    void testRefreshToken_UserNotFound_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
        when(jwtService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TEST_REFRESH_TOKEN)).thenReturn("test@example.com");
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token refresh failed"))
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    void testRefreshToken_DisabledUser_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        testUser.setEnabled(false);
        
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenReturn(TEST_REFRESH_TOKEN);
        when(jwtService.validateRefreshToken(TEST_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TEST_REFRESH_TOKEN)).thenReturn("test@example.com");
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token refresh failed"))
                .andExpect(jsonPath("$.error").value("User account is disabled"));

        verify(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    void testRefreshToken_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(cookieService.getRefreshTokenFromCookies(any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("Cookie parsing failed"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/refresh-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token refresh failed"))
                .andExpect(jsonPath("$.error").value("Cookie parsing failed"));

        verify(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    void testLogout_ShouldClearCookieAndReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    void testLogout_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Cookie clearing failed"))
                .when(cookieService).clearRefreshTokenCookie(any(HttpServletResponse.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/logout"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Logout failed"))
                .andExpect(jsonPath("$.error").value("Cookie clearing failed"));
    }

    // Validation tests for request DTOs
    @Test
    void testRegister_EmptyEmail_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("");
        registerRequest.setPassword("StrongPass123");
        registerRequest.setRole("PATIENT");

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_EmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("");
        registerRequest.setRole("PATIENT");

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_EmptyEmail_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("");
        loginRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_EmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }
}
