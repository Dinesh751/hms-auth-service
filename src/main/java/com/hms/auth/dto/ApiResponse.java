package com.hms.auth.dto;

import java.time.LocalDateTime;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ApiResponse<T>{
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timeStamp;

    // success response factory method
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    // success response factory method without data
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }

    // failure response factory method
    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(false, message, data, LocalDateTime.now());
    }

    // failure response factory method without data
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}