package com.hms.auth.config;

import com.hms.auth.entity.User;
import com.hms.auth.service.JwtService;
import com.hms.auth.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1️⃣ Check if this is a public endpoint
        if(isPublicEndpoint(request.getRequestURI())) {
            log.debug("Public endpoint accessed: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2️⃣ Extract token from request
            String token = extractTokenFromRequest(request);
            
            // 3️⃣ Validate token and set authentication
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtService.validateAccessToken(token)) {
                    // 4️⃣ Extract user information
                    String email = jwtService.extractEmail(token);
                    String role = jwtService.extractRole(token);
                        
                    // 5️⃣ Find user in database
                    Optional<User> userOpt = userService.findByEmail(email);
                    if (userOpt.isEmpty()) {
                        log.debug("No user found with email: {}", email);
                        filterChain.doFilter(request, response);
                        return; // ✅ Continue without authentication
                    }
                    
                    User user = userOpt.get();

                    // 6️⃣ Check if user is enabled
                    if (!user.getEnabled()) {
                        log.warn("User account disabled: {}", email);
                        filterChain.doFilter(request, response);
                        return; // ✅ Continue without authentication
                    }

                    // 7️⃣ Create authorities
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                    // 8️⃣ Set user authentication in security context
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null, authorities
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 9️⃣ Add user info to request attributes
                    request.setAttribute("currentUser", user);
                    request.setAttribute("currentUserId", user.getId());
                    request.setAttribute("currentUserRole", user.getRole());
                                
                    log.debug("User authenticated: {} with role: {}", email, role);
                } else {
                    log.debug("Invalid JWT token");
                }
            } else if (token == null) {
                log.debug("No JWT token found in request");
            }

        } catch (Exception ex) {
            log.error("Cannot set user authentication: ", ex);
            SecurityContextHolder.clearContext();
        }

        // 🔟 Always continue the filter chain
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicEndpoint(String requestPath) {
        String[] publicEndpoints = {
            "/api/auth/v1/register",
            "/api/auth/v1/login", 
            "/api/auth/v1/refresh-token",
            "/api/auth/v1/logout",
            "/api/auth/v1/health",
            "/error",
            "/actuator"
        };
        
        for (String endpoint : publicEndpoints) {
            if (requestPath.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
}
