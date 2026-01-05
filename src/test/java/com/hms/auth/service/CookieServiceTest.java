package com.hms.auth.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.refresh-token-expiration=604800",
    "app.cookie.refresh-token.name=test_refresh_token",
    "app.cookie.refresh-token.path=/api/auth/v1",
    "app.cookie.refresh-token.secure=false",
    "app.cookie.refresh-token.http-only=true",
    "app.cookie.refresh-token.same-site=Strict",
    "app.cookie.refresh-token.domain="
})
class CookieServiceTest {

    @Autowired
    private CookieService cookieService;

    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    private final String TEST_TOKEN = "test.refresh.token.value";

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
    }

    @Test
    void testCreateRefreshTokenCookie_ShouldCreateCookieWithCorrectProperties() {
        // Act
        cookieService.createRefreshTokenCookie(response, TEST_TOKEN);

        // Assert - Check Set-Cookie header (the actual cookie that gets sent)
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("test_refresh_token=" + TEST_TOKEN);
        assertThat(setCookieHeader).contains("Path=/api/auth/v1");
        assertThat(setCookieHeader).contains("Max-Age=604800");
        assertThat(setCookieHeader).contains("HttpOnly");
        
        // Also check the basic cookie object (first one added)
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSizeGreaterThan(0);

        Cookie cookie = cookies[0];
        assertThat(cookie.getName()).isEqualTo("test_refresh_token");
        assertThat(cookie.getValue()).isEqualTo(TEST_TOKEN);
        assertThat(cookie.getPath()).isEqualTo("/api/auth/v1");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getMaxAge()).isEqualTo(604800);
    }

    @Test
    void testCreateRefreshTokenCookie_ShouldAddSetCookieHeader() {
        // Act
        cookieService.createRefreshTokenCookie(response, TEST_TOKEN);

        // Assert - Check that we have Set-Cookie headers
        assertThat(response.getHeaders("Set-Cookie")).hasSizeGreaterThan(0);
        
        // Find the Set-Cookie header with SameSite attribute (should be the enhanced one)
        boolean foundSameSiteHeader = response.getHeaders("Set-Cookie").stream()
            .anyMatch(header -> header.contains("SameSite=Strict"));
        
        assertThat(foundSameSiteHeader).as("Should have a Set-Cookie header with SameSite=Strict").isTrue();
        
        // Check that at least one header contains all expected attributes
        String enhancedHeader = response.getHeaders("Set-Cookie").stream()
            .filter(header -> header.contains("SameSite=Strict"))
            .findFirst()
            .orElse("");
            
        assertThat(enhancedHeader).contains("test_refresh_token=" + TEST_TOKEN);
        assertThat(enhancedHeader).contains("Path=/api/auth/v1");
        assertThat(enhancedHeader).contains("Max-Age=604800");
        assertThat(enhancedHeader).contains("HttpOnly");
        assertThat(enhancedHeader).doesNotContain("Secure"); // Since secure=false
    }

    @Test
    void testCreateRefreshTokenCookie_WithNullToken_ShouldCreateEmptyValueCookie() {
        // Act
        cookieService.createRefreshTokenCookie(response, null);

        // Assert - Check the basic cookie (first one)
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSizeGreaterThan(0);

        Cookie cookie = cookies[0];
        assertThat(cookie.getName()).isEqualTo("test_refresh_token");
        assertThat(cookie.getValue()).isNull();
    }

    @Test
    void testCreateRefreshTokenCookie_WithEmptyToken_ShouldCreateEmptyValueCookie() {
        // Act
        cookieService.createRefreshTokenCookie(response, "");

        // Assert - Check the basic cookie (first one)
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSizeGreaterThan(0);

        Cookie cookie = cookies[0];
        assertThat(cookie.getName()).isEqualTo("test_refresh_token");
        assertThat(cookie.getValue()).isEqualTo("");
    }

    @Test
    void testClearRefreshTokenCookie_ShouldCreateExpiredCookie() {
        // Act
        cookieService.clearRefreshTokenCookie(response);

        // Assert - Check the basic cookie (first one)
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSizeGreaterThan(0);

        Cookie cookie = cookies[0];
        assertThat(cookie.getName()).isEqualTo("test_refresh_token");
        assertThat(cookie.getValue()).isEqualTo("");
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.getPath()).isEqualTo("/api/auth/v1");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
    }

    @Test
    void testClearRefreshTokenCookie_ShouldAddSetCookieHeaderWithMaxAge0() {
        // Act
        cookieService.clearRefreshTokenCookie(response);

        // Assert
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("test_refresh_token=");
        assertThat(setCookieHeader).contains("Max-Age=0");
        assertThat(setCookieHeader).contains("Path=/api/auth/v1");
    }

    @Test
    void testGetRefreshTokenFromCookies_ValidCookie_ShouldReturnToken() {
        // Arrange
        Cookie refreshCookie = new Cookie("test_refresh_token", TEST_TOKEN);
        request.setCookies(refreshCookie);

        // Act
        String token = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(token).isEqualTo(TEST_TOKEN);
    }

    @Test
    void testGetRefreshTokenFromCookies_NoCookies_ShouldReturnNull() {
        // Arrange - request has no cookies

        // Act
        String token = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(token).isNull();
    }

    @Test
    void testGetRefreshTokenFromCookies_NoMatchingCookie_ShouldReturnNull() {
        // Arrange
        Cookie otherCookie = new Cookie("other_cookie", "other_value");
        request.setCookies(otherCookie);

        // Act
        String token = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(token).isNull();
    }

    @Test
    void testGetRefreshTokenFromCookies_MultipleCounties_ShouldReturnCorrectToken() {
        // Arrange
        Cookie otherCookie = new Cookie("other_cookie", "other_value");
        Cookie refreshCookie = new Cookie("test_refresh_token", TEST_TOKEN);
        Cookie anotherCookie = new Cookie("another_cookie", "another_value");
        request.setCookies(otherCookie, refreshCookie, anotherCookie);

        // Act
        String token = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(token).isEqualTo(TEST_TOKEN);
    }

    @Test
    void testGetRefreshTokenFromCookies_EmptyTokenValue_ShouldReturnEmptyString() {
        // Arrange
        Cookie refreshCookie = new Cookie("test_refresh_token", "");
        request.setCookies(refreshCookie);

        // Act
        String token = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(token).isEqualTo("");
    }

    @Test
    void testGetCookieConfigSummary_ShouldReturnCorrectSummary() {
        // Act
        String summary = cookieService.getCookieConfigSummary();

        // Assert
        assertThat(summary).contains("test_refresh_token");
        assertThat(summary).contains("/api/auth/v1");
        assertThat(summary).contains("secure=false");
        assertThat(summary).contains("httpOnly=true");
        assertThat(summary).contains("sameSite=Strict");
        assertThat(summary).contains("maxAge=604800");
        assertThat(summary).contains("domain=not-set");
    }

    @Test
    void testIsConfigurationValid_ValidConfig_ShouldReturnTrue() {
        // Act
        boolean isValid = cookieService.isConfigurationValid();

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testCreateAndRetrieveCookie_FullWorkflow() {
        // Act - Create cookie
        cookieService.createRefreshTokenCookie(response, TEST_TOKEN);

        // Simulate browser sending the cookie back
        Cookie[] cookies = response.getCookies();
        Cookie createdCookie = cookies[0];
        request.setCookies(createdCookie);

        // Act - Retrieve token
        String retrievedToken = cookieService.getRefreshTokenFromCookies(request);

        // Assert
        assertThat(retrievedToken).isEqualTo(TEST_TOKEN);
    }

    @Test
    void testClearCookieAfterCreation_ShouldOverridePreviousCookie() {
        // Arrange - Create a cookie first
        cookieService.createRefreshTokenCookie(response, TEST_TOKEN);
        
        // Reset response to simulate new response
        response = new MockHttpServletResponse();

        // Act - Clear the cookie
        cookieService.clearRefreshTokenCookie(response);

        // Assert - Check the basic cookie (first one)
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).hasSizeGreaterThan(0);
        
        Cookie clearedCookie = cookies[0];
        assertThat(clearedCookie.getValue()).isEqualTo("");
        assertThat(clearedCookie.getMaxAge()).isEqualTo(0);
    }

    @Test
    void testCookieSecurityHeaders_WithSecureFalse() {
        // Act
        cookieService.createRefreshTokenCookie(response, TEST_TOKEN);

        // Assert - Find the enhanced Set-Cookie header with SameSite
        String enhancedHeader = response.getHeaders("Set-Cookie").stream()
            .filter(header -> header.contains("SameSite=Strict"))
            .findFirst()
            .orElse("");
            
        assertThat(enhancedHeader).isNotEmpty();
        assertThat(enhancedHeader).doesNotContain("Secure");
        assertThat(enhancedHeader).contains("HttpOnly");
        assertThat(enhancedHeader).contains("SameSite=Strict");
    }

    @Test
    void testMultipleCookieOperations() {
        // Test creating multiple cookies in sequence
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockHttpServletResponse response3 = new MockHttpServletResponse();

        // Create cookies
        cookieService.createRefreshTokenCookie(response1, "token1");
        cookieService.createRefreshTokenCookie(response2, "token2");
        cookieService.clearRefreshTokenCookie(response3);

        // Assert each response has the correct cookie
        assertThat(response1.getCookies()[0].getValue()).isEqualTo("token1");
        assertThat(response2.getCookies()[0].getValue()).isEqualTo("token2");
        assertThat(response3.getCookies()[0].getValue()).isEqualTo("");
        assertThat(response3.getCookies()[0].getMaxAge()).isEqualTo(0);
    }
}
