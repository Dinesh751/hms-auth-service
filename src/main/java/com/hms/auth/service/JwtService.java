package com.hms.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hms.auth.entity.User;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    //  Generate Access Token (short-lived, contains user data)
    
    public String generateAccessToken(User user) {
        log.info("Generating access token for user: {}", user.getEmail());
        
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);

        return Jwts.builder()
            .subject(user.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("userId", user.getId().toString())
            .claim("role", user.getRole().name())
            .claim("enabled", user.getEnabled())
            .claim("tokenType", "ACCESS")
            .signWith(getSigningKey())
            .compact();
    }

    //  Generate Refresh Token (long-lived, minimal data)
    public String generateRefreshToken(User user) {
        log.info("Generating refresh token for user: {}", user.getEmail());
        
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);
        String tokenId = UUID.randomUUID().toString();
        
        return Jwts.builder()
            .id(tokenId)  // JTI (JWT ID)
            .subject(user.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("userId", user.getId().toString())
            .claim("tokenType", "REFRESH")
            .signWith(getSigningKey())
            .compact();
    }

    //  Extract email (subject) from token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //  Extract user ID from token
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    //  Extract user role from token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    //  Extract token ID (for refresh tokens)
    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    //  Extract token type (ACCESS or REFRESH)
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    //  Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //  Check if token is expired
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    //  Validate access token (checks structure, signature, expiration, and type)
    public boolean validateAccessToken(String token) {
        try {
            if (!validateTokenStructure(token)) {
                return false;
            }

            String tokenType = extractTokenType(token);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("Token is not an access token: {}", tokenType);
                return false;
            }
            
            if (isTokenExpired(token)) {
                log.warn("Access token is expired");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating access token: {}", e.getMessage());
            return false;
        }
    }

    //  Validate refresh token (checks structure, signature, expiration, and type)
    public boolean validateRefreshToken(String token) {
        try {
            if (!validateTokenStructure(token)) {
                return false;
            }

            String tokenType = extractTokenType(token);
            if (!"REFRESH".equals(tokenType)) {
                log.warn("Token is not a refresh token: {}", tokenType);
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("Refresh token is expired");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage());
            return false;
        }
    }

    //  Validate token structure and signature (JJWT 0.12+ Modern API)
    public boolean validateTokenStructure(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    //  Get remaining time until token expiry (in seconds)
    public long getTokenExpiryTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long currentTime = System.currentTimeMillis();
            long expiryTime = expiration.getTime();
            return Math.max(0, (expiryTime - currentTime) / 1000);
        } catch (Exception e) {
            log.warn("Error getting token expiry time: {}", e.getMessage());
            return 0;
        }
    }

    //  Get access token expiration time (for testing)
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    /**
     * Get refresh token expiration time (for testing)
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
    
    /**
     * Get JWT secret length (for testing)
     */
    public int getSecretLength() {
        return jwtSecret.length();
    }
    
    /**
     * Check if a token is about to expire (within 5 minutes)
     */
    public boolean isTokenAboutToExpire(String token) {
        return getTokenExpiryTime(token) <= 300; // 5 minutes
    }

    /**
     * Extract a specific claim from token
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extract all claims from token (JJWT 0.12+ Modern API)
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
