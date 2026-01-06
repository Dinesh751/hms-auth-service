package com.hms.auth.controller;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.time.LocalDateTime;

// ✅ Spring Boot utilities
import org.springframework.beans.factory.annotation.Autowired;

// ✅ Metrics
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.hms.auth.service.JwtService;
import com.hms.auth.service.UserService;
import com.hms.auth.service.CookieService;
import com.hms.auth.dto.ApiResponse;
import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.RegisterRequest;
import com.hms.auth.dto.TokenResponse;
import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {
    
   private UserService userService;
   private JwtService jwtService;
   private CookieService cookieService;
   
   // Metrics beans
   @Autowired
   private Counter loginSuccessCounter;
   
   @Autowired
   private Counter loginFailureCounter;
   
   @Autowired
   private Counter userRegistrationCounter;
   
   @Autowired
   private Counter jwtTokenCounter;
   
   @Autowired
   private Timer authenticationTimer;

   @Autowired
   public AuthController(UserService userService, JwtService jwtService, CookieService cookieService) {
       this.userService = userService;
       this.jwtService = jwtService;
       this.cookieService = cookieService;
   }

   @GetMapping("/health")
   public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
       Map<String, Object> healthData = new HashMap<>();
       healthData.put("status", "UP");
       healthData.put("timestamp", LocalDateTime.now());
       return ResponseEntity.ok(ApiResponse.success("Service is healthy",healthData));
   }

   @PostMapping("/register")
   public ResponseEntity<ApiResponse<TokenResponse>> register(
    @Valid @RequestBody RegisterRequest registerRequest,
    HttpServletResponse response
   ){
      
    try{
        String roleString = registerRequest.getRole();
        UserRole role;

        try{
            role = UserRole.valueOf(roleString.trim().toUpperCase());
        }catch( IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.<TokenResponse> error("Invalid role "+ roleString, e.getMessage() ));
        }

        User user = userService.registerUser(registerRequest.getEmail(), registerRequest.getPassword(), role);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Track successful registration and token generation
        userRegistrationCounter.increment();
        jwtTokenCounter.increment(2); // access + refresh token

        cookieService.createRefreshTokenCookie(response,refreshToken);

        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.getRole().name(),
            user.getEnabled()
        );

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setUser(userInfo);
        tokenResponse.setExpiresIn(jwtService.getTokenExpiryTime(accessToken));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<TokenResponse> success("User registered successfully", tokenResponse));

    }catch(IllegalArgumentException e){
    // Handle validation errors (email already exists, etc.)
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.<TokenResponse>error("Registration failed", e.getMessage()));
    }
    catch(Exception e){
       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<TokenResponse>error("Registration failed", e.getMessage()));
    }
   }

   @PostMapping("/login")
   public ResponseEntity<ApiResponse<TokenResponse>> login(
    @Valid @RequestBody LoginRequest requestBody,
    HttpServletResponse response
   ) {
       Timer.Sample sample = Timer.start();
       try {
           Optional<User> userOpt = userService.authenticateUser(requestBody.getEmail(), requestBody.getPassword());

           // ✅ Check if authentication was successful
           if (userOpt.isEmpty()) {
               loginFailureCounter.increment();
               return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                   .body(ApiResponse.<TokenResponse>error("Authentication failed", "Invalid email or password"));
           }
        
           User user = userOpt.get();

           String accessToken = jwtService.generateAccessToken(user);
           String refreshToken = jwtService.generateRefreshToken(user);
           
           // Track successful operations
           loginSuccessCounter.increment();
           jwtTokenCounter.increment(2); // access + refresh token

           cookieService.createRefreshTokenCookie(response, refreshToken);

           TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
               user.getId().toString(),
               user.getEmail(),
               user.getRole().name(),
               user.getEnabled()
           );

           TokenResponse tokenResponse = new TokenResponse();
           tokenResponse.setAccessToken(accessToken);
           tokenResponse.setUser(userInfo);
           tokenResponse.setExpiresIn(jwtService.getTokenExpiryTime(accessToken));

           return ResponseEntity.ok(ApiResponse.success("Login successful", tokenResponse));
       } catch (IllegalArgumentException e) {
           loginFailureCounter.increment();
           return ResponseEntity.status(HttpStatus.BAD_REQUEST)
               .body(ApiResponse.error("Login failed", e.getMessage()));
       } catch (Exception e) {
           loginFailureCounter.increment();
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(ApiResponse.error("Login failed", e.getMessage()));
       } finally {
           sample.stop(authenticationTimer);
       }
   }

   @PostMapping("/refresh-token")
   public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
    HttpServletRequest request,
    HttpServletResponse response
   ){

    try{

        String refreshToken = cookieService.getRefreshTokenFromCookies(request);

        if(refreshToken == null || refreshToken.isEmpty()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<TokenResponse> error("Token refresh failed", "No refresh token found"));
        }

        if(!jwtService.validateRefreshToken(refreshToken)){
            cookieService.clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<TokenResponse>error("Token refresh failed", "Invalid or expired refresh token"));
        }

        String email = jwtService.extractEmail(refreshToken);
        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty()) {
            // Clear refresh token cookie if user not found
            cookieService.clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<TokenResponse>error("Token refresh failed", "User not found"));
        }

        User user = userOpt.get();

        if (!user.getEnabled()) {
            cookieService.clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<TokenResponse>error("Token refresh failed", "User account is disabled"));
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        cookieService.createRefreshTokenCookie(response, newRefreshToken);

        TokenResponse.UserInfo userInfo = new TokenResponse.UserInfo(
            user.getId().toString(),
            user.getEmail(),
            user.getRole().name(),
            user.getEnabled()
        );

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(newAccessToken);
        tokenResponse.setUser(userInfo);
        tokenResponse.setExpiresIn(jwtService.getTokenExpiryTime(newAccessToken));

        return ResponseEntity.ok(ApiResponse.success("Token refresh successful", tokenResponse));
    } catch (Exception e) {
        cookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.<TokenResponse> error("Token refresh failed", e.getMessage()));
    }
   }

   @PostMapping("/logout")
   public ResponseEntity<ApiResponse<Void>> logout(
    HttpServletResponse response
   ){
    try{

        cookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.<Void> success("Logout successful"));
    }catch(Exception e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.<Void> error("Logout failed", e.getMessage()));
    }
   }

}

