package com.hms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;  // Access token expiry in seconds
    private UserInfo user;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String role;
        private boolean enabled;
    }
}
