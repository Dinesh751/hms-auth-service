package com.hms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String error;
    
    // ✅ Success response
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    // ✅ Error response
    public ApiResponse(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.data = null; // ✅ Explicitly set data to null for errors
    }
    
    // ✅ Static helper methods that return correct generic types
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, (T) null);
    }
    
    // ✅ FIX: Type-safe error methods
    public static <T> ApiResponse<T> error(String message, String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setError(error);
        response.setTimestamp(LocalDateTime.now());
        response.setData(null);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return error(message, (String) null);
    }
}