package com.hms.auth.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=myVerySecretKeyForTestingPurposesOnly123456789",
    "jwt.access-token-expiration=3600",
    "jwt.refresh-token-expiration=604800"
})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

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
    void testGenerateAccessToken_ShouldCreateValidToken() {
        // Act
        String token = jwtService.generateAccessToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token content
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId().toString());
        assertThat(jwtService.extractRole(token)).isEqualTo(testUser.getRole().name());
        assertThat(jwtService.extractTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    void testGenerateRefreshToken_ShouldCreateValidToken() {
        // Act
        String token = jwtService.generateRefreshToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token content
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId().toString());
        assertThat(jwtService.extractTokenType(token)).isEqualTo("REFRESH");
        assertThat(jwtService.extractTokenId(token)).isNotNull();
    }

    @Test
    void testExtractEmail_ShouldReturnCorrectEmail() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String extractedEmail = jwtService.extractEmail(token);

        // Assert
        assertThat(extractedEmail).isEqualTo(testUser.getEmail());
    }

    @Test
    void testExtractUserId_ShouldReturnCorrectUserId() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String extractedUserId = jwtService.extractUserId(token);

        // Assert
        assertThat(extractedUserId).isEqualTo(testUser.getId().toString());
    }

    @Test
    void testExtractRole_ShouldReturnCorrectRole() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String extractedRole = jwtService.extractRole(token);

        // Assert
        assertThat(extractedRole).isEqualTo(testUser.getRole().name());
    }

    @Test
    void testExtractTokenId_ShouldReturnTokenIdForRefreshToken() {
        // Arrange
        String token = jwtService.generateRefreshToken(testUser);

        // Act
        String tokenId = jwtService.extractTokenId(token);

        // Assert
        assertThat(tokenId).isNotNull();
        assertThat(tokenId).isNotEmpty();
    }

    @Test
    void testExtractTokenId_ShouldReturnNullForAccessToken() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String tokenId = jwtService.extractTokenId(token);

        // Assert
        assertThat(tokenId).isNull();
    }

    @Test
    void testExtractTokenType_AccessToken_ShouldReturnACCESS() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        String tokenType = jwtService.extractTokenType(token);

        // Assert
        assertThat(tokenType).isEqualTo("ACCESS");
    }

    @Test
    void testExtractTokenType_RefreshToken_ShouldReturnREFRESH() {
        // Arrange
        String token = jwtService.generateRefreshToken(testUser);

        // Act
        String tokenType = jwtService.extractTokenType(token);

        // Assert
        assertThat(tokenType).isEqualTo("REFRESH");
    }

    @Test
    void testExtractExpiration_ShouldReturnFutureDate() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        Date expiration = jwtService.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void testIsTokenExpired_ValidToken_ShouldReturnFalse() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        Boolean isExpired = jwtService.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    @Test
    void testIsTokenExpired_InvalidToken_ShouldReturnTrue() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        Boolean isExpired = jwtService.isTokenExpired(invalidToken);

        // Assert
        assertThat(isExpired).isTrue();
    }

    @Test
    void testValidateAccessToken_ValidToken_ShouldReturnTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        boolean isValid = jwtService.validateAccessToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateAccessToken_RefreshToken_ShouldReturnFalse() {
        // Arrange
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Act
        boolean isValid = jwtService.validateAccessToken(refreshToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateAccessToken_InvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtService.validateAccessToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateRefreshToken_ValidToken_ShouldReturnTrue() {
        // Arrange
        String token = jwtService.generateRefreshToken(testUser);

        // Act
        boolean isValid = jwtService.validateRefreshToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateRefreshToken_AccessToken_ShouldReturnFalse() {
        // Arrange
        String accessToken = jwtService.generateAccessToken(testUser);

        // Act
        boolean isValid = jwtService.validateRefreshToken(accessToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateRefreshToken_InvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtService.validateRefreshToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenStructure_ValidToken_ShouldReturnTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        boolean isValid = jwtService.validateTokenStructure(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateTokenStructure_InvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtService.validateTokenStructure(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenStructure_NullToken_ShouldReturnFalse() {
        // Act
        boolean isValid = jwtService.validateTokenStructure(null);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenStructure_EmptyToken_ShouldReturnFalse() {
        // Act
        boolean isValid = jwtService.validateTokenStructure("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testGetTokenExpiryTime_ValidToken_ShouldReturnPositiveValue() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        long expiryTime = jwtService.getTokenExpiryTime(token);

        // Assert
        assertThat(expiryTime).isPositive();
        assertThat(expiryTime).isLessThanOrEqualTo(3600); // Max expiration time
    }

    @Test
    void testGetTokenExpiryTime_InvalidToken_ShouldReturnZero() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        long expiryTime = jwtService.getTokenExpiryTime(invalidToken);

        // Assert
        assertThat(expiryTime).isEqualTo(0);
    }

    @Test
    void testGetAccessTokenExpiration_ShouldReturnConfiguredValue() {
        // Act
        long expiration = jwtService.getAccessTokenExpiration();

        // Assert
        assertThat(expiration).isEqualTo(3600);
    }

    @Test
    void testGetRefreshTokenExpiration_ShouldReturnConfiguredValue() {
        // Act
        long expiration = jwtService.getRefreshTokenExpiration();

        // Assert
        assertThat(expiration).isEqualTo(604800);
    }

    @Test
    void testGetSecretLength_ShouldReturnCorrectLength() {
        // Act
        int secretLength = jwtService.getSecretLength();

        // Assert
        assertThat(secretLength).isGreaterThan(0);
        assertThat(secretLength).isEqualTo("myVerySecretKeyForTestingPurposesOnly123456789".length());
    }

    @Test
    void testIsTokenAboutToExpire_NewToken_ShouldReturnFalse() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);

        // Act
        boolean isAboutToExpire = jwtService.isTokenAboutToExpire(token);

        // Assert
        assertThat(isAboutToExpire).isFalse();
    }

    @Test
    void testIsTokenAboutToExpire_InvalidToken_ShouldReturnTrue() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isAboutToExpire = jwtService.isTokenAboutToExpire(invalidToken);

        // Assert
        assertThat(isAboutToExpire).isTrue();
    }

    @Test
    void testGenerateTokensForDifferentRoles_ShouldWork() {
        // Arrange
        User doctorUser = new User();
        doctorUser.setId(UUID.randomUUID());
        doctorUser.setEmail("doctor@example.com");
        doctorUser.setRole(UserRole.DOCTOR);
        doctorUser.setEnabled(true);

        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);

        // Act
        String patientToken = jwtService.generateAccessToken(testUser);
        String doctorToken = jwtService.generateAccessToken(doctorUser);
        String adminToken = jwtService.generateAccessToken(adminUser);

        // Assert
        assertThat(jwtService.extractRole(patientToken)).isEqualTo("PATIENT");
        assertThat(jwtService.extractRole(doctorToken)).isEqualTo("DOCTOR");
        assertThat(jwtService.extractRole(adminToken)).isEqualTo("ADMIN");
    }

    @Test
    void testGenerateMultipleTokens_ShouldHaveDifferentIds() {
        // Act
        String token1 = jwtService.generateRefreshToken(testUser);
        String token2 = jwtService.generateRefreshToken(testUser);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractTokenId(token1))
            .isNotEqualTo(jwtService.extractTokenId(token2));
    }

    @Test
    void testExtractClaims_WithDisabledUser_ShouldWork() {
        // Arrange
        testUser.setEnabled(false);
        String token = jwtService.generateAccessToken(testUser);

        // Act & Assert
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.validateTokenStructure(token)).isTrue();
        assertThat(jwtService.validateAccessToken(token)).isTrue();
    }
}
