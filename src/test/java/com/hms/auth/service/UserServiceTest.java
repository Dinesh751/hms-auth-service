package com.hms.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;
import com.hms.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

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
    void testRegisterUser_ValidInput_ShouldSucceed() {
        // Arrange
        String email = "test@example.com";
        String password = "StrongPass123";
        UserRole role = UserRole.PATIENT;

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(email, password, role);

        // Assert
        assertNotNull(result);
        assertEquals(email.toLowerCase(), testUser.getEmail());
        verify(userRepository).existsByEmail(email.toLowerCase());
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_InvalidEmail_ShouldThrowException() {
        // Arrange
        String invalidEmail = "invalid-email";
        String password = "StrongPass123";
        UserRole role = UserRole.PATIENT;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(invalidEmail, password, role)
        );
        
        assertEquals("Invalid email format: " + invalidEmail, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_WeakPassword_ShouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String weakPassword = "weak";
        UserRole role = UserRole.PATIENT;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(email, weakPassword, role)
        );
        
        assertEquals("Password must be at least 8 characters and contain uppercase, lowercase and digit", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_ExistingUser_ShouldThrowException() {
        // Arrange
        String email = "test@example.com";
        String password = "StrongPass123";
        UserRole role = UserRole.PATIENT;

        when(userRepository.existsByEmail(email.toLowerCase())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(email, password, role)
        );
        
        assertEquals("User with email already exists: " + email, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAuthenticateUser_ValidCredentials_ShouldSucceed() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";

        when(userRepository.findByEmailAndEnabled(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

        // Act
        Optional<User> result = userService.authenticateUser(email, password);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void testAuthenticateUser_InvalidPassword_ShouldReturnEmpty() {
        // Arrange
        String email = "test@example.com";
        String wrongPassword = "wrongpassword";

        when(userRepository.findByEmailAndEnabled(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        // Act
        Optional<User> result = userService.authenticateUser(email, wrongPassword);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testDisableUser_ValidUser_ShouldSucceed() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.disableUser(email);

        // Assert
        assertFalse(testUser.getEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdatePassword_ValidInput_ShouldSucceed() {
        // Arrange
        String email = "test@example.com";
        String newPassword = "NewStrongPass123";
        
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.updatePassword(email, newPassword);

        // Assert
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void testGetUserCount_ShouldReturnCount() {
        // Arrange
        when(userRepository.count()).thenReturn(5L);

        // Act
        long count = userService.count();

        // Assert
        assertEquals(5L, count);
        verify(userRepository).count();
    }
}
