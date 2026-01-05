package com.hms.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean 
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ 1. Disable CSRF for REST APIs
            .csrf(csrf -> csrf.disable())
            
            // ✅ 2. Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ 3. Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // ✅ 4. Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow all auth endpoints
                .requestMatchers("/api/auth/v1/register").permitAll()
                .requestMatchers("/api/auth/v1/login").permitAll() 
                .requestMatchers("/api/auth/v1/refresh-token").permitAll()
                .requestMatchers("/api/auth/v1/logout").permitAll()
                .requestMatchers("/api/auth/v1/health").permitAll()
                
                // Allow error endpoints
                .requestMatchers("/error").permitAll()
                
                // Allow actuator endpoints (if you have them)
                .requestMatchers("/actuator/**").permitAll()
                
                // All other requests need authentication
                .anyRequest().authenticated()
            );
            
        return http.build();
    }
    
    // ✅ 5. Add CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (update for production)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (important for cookies)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
