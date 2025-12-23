package com.hms.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    @GetMapping("/health")
    public String health() {
        return "HMS Auth Service is running!";
    }
    
    @GetMapping("/")
    public String home() {
        return "Welcome to HMS Auth Service";
    }
}
