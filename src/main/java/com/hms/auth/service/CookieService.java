package com.hms.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Slf4j  // ✅ Add logging for security monitoring
public class CookieService {

    // ✅ Configuration from application.yaml
    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    @Value("${app.cookie.refresh-token.name:refresh_token}")
    private String cookieName;

    @Value("${app.cookie.refresh-token.path:/api/auth/v1}")
    private String cookiePath;

    @Value("${app.cookie.refresh-token.secure:false}")  // false for dev, true for prod
    private boolean secure;

    @Value("${app.cookie.refresh-token.http-only:true}")
    private boolean httpOnly;

    @Value("${app.cookie.refresh-token.same-site:Strict}")
    private String sameSite;

    @Value("${app.cookie.refresh-token.domain:}")
    private String domain;

    
    public void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        log.debug("Creating refresh token cookie with path: {}, secure: {}", cookiePath, secure);
        
        Cookie cookie = new Cookie(cookieName, refreshToken);
        
        // Apply security settings
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(cookiePath);
        cookie.setMaxAge((int) refreshTokenExpiration);
        
        // Set domain if configured
        if (isValidDomain(domain)) {
            cookie.setDomain(domain);
        }
        
        // ✅ Add cookie with enhanced security headers
        addSecureCookie(response, cookie);
        
        log.info("Refresh token cookie created successfully - secure: {}, httpOnly: {}, sameSite: {}", 
                 secure, httpOnly, sameSite);
    }

    /**
     * Clear refresh token cookie securely
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        log.debug("Clearing refresh token cookie");
        
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0);  // Immediate deletion
        
        if (isValidDomain(domain)) {
            cookie.setDomain(domain);
        }
        
        addSecureCookie(response, cookie);
        log.info("Refresh token cookie cleared successfully");
    }


    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("No cookies found in request");
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                log.debug("Found refresh token cookie");
                return cookie.getValue();
            }
        }
        
        log.debug("Refresh token cookie not found");
        return null;
    }

   
    private void addSecureCookie(HttpServletResponse response, Cookie cookie) {
        // Set basic cookie
        response.addCookie(cookie);
        
        // Build enhanced Set-Cookie header with SameSite
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue());
        cookieHeader.append("; Path=").append(cookie.getPath());
        cookieHeader.append("; Max-Age=").append(cookie.getMaxAge());
        
        if (httpOnly) {
            cookieHeader.append("; HttpOnly");
        }
        
        if (secure) {
            cookieHeader.append("; Secure");
        }
        
        if (cookie.getDomain() != null) {
            cookieHeader.append("; Domain=").append(cookie.getDomain());
        }
        
        // ✅ Add SameSite for CSRF protection
        if (isValidSameSite(sameSite)) {
            cookieHeader.append("; SameSite=").append(sameSite);
        }
        
        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    
    private boolean isValidDomain(String domain) {
        return domain != null && !domain.trim().isEmpty();
    }

    
    private boolean isValidSameSite(String sameSite) {
        if (sameSite == null) return false;
        String normalized = sameSite.trim().toLowerCase();
        return "strict".equals(normalized) || "lax".equals(normalized) || "none".equals(normalized);
    }

   
    public String getCookieConfigSummary() {
        return String.format(
            "Cookie[name=%s, path=%s, secure=%s, httpOnly=%s, sameSite=%s, domain=%s, maxAge=%d]",
            cookieName, cookiePath, secure, httpOnly, sameSite, 
            isValidDomain(domain) ? domain : "not-set", refreshTokenExpiration
        );
    }

    /**
     * ✅ Validate cookie configuration on startup
     */
    public boolean isConfigurationValid() {
        if (cookieName == null || cookieName.trim().isEmpty()) {
            log.error("Cookie name is not configured properly");
            return false;
        }
        
        if (cookiePath == null || cookiePath.trim().isEmpty()) {
            log.error("Cookie path is not configured properly");
            return false;
        }
        
        if (!isValidSameSite(sameSite)) {
            log.warn("Invalid SameSite value: {}. Should be Strict, Lax, or None", sameSite);
        }
        
        log.info("Cookie configuration validated successfully: {}", getCookieConfigSummary());
        return true;
    }
}
