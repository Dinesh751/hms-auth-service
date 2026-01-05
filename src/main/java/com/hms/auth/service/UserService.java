package com.hms.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.auth.entity.User;
import com.hms.auth.entity.UserRole;
import com.hms.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate password strength
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for at least one digit, one lowercase, one uppercase letter
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        
        return hasDigit && hasLower && hasUpper;
    }

    public User registerUser(String email, String rawPassword , UserRole role) throws IllegalArgumentException {

        log.info("Registering user with email: {}", email);

        // Validate email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        // Validate password strength
        if (!isValidPassword(rawPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase and digit");
        }

        if(userRepository.existsByEmail(email.toLowerCase())){
            throw new IllegalArgumentException("User with email already exists: " + email);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(email.toLowerCase(), encodedPassword, role);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email){
        log.info("Fetching user by email: {}", email);

        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticateUser(String email, String rawPassword){
        log.info("Authenticating user with email: {}", email);

        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if(rawPassword == null || rawPassword.isEmpty()){
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        Optional<User> foundUser = userRepository.findByEmailAndEnabled(email.toLowerCase());

        if(foundUser.isEmpty()){
            log.warn("User not found or disabled: {}", email);
            return Optional.empty();
        }

        if(!passwordEncoder.matches(rawPassword, foundUser.get().getPassword())){
            log.warn("Invalid password for user: {}", email);
            return Optional.empty();
        }

        log.info("User authenticated successfully: {}", email);
        return Optional.of(foundUser.get());
    }

    public void enableUser(String email){
        log.info("Enabling user with email: {}", email);

        Optional<User> foundUser = userRepository.findByEmail(email.toLowerCase());

        if(foundUser.isEmpty()){
            throw new IllegalArgumentException("User not found: " + email);
        }

        User user = foundUser.get();
        user.setEnabled(true);
        userRepository.save(user);

        log.info("User enabled successfully: {}", email);
    }
    
    /**
     * Disable a user account
     */
    public void disableUser(String email) {
        log.info("Disabling user with email: {}", email);

        Optional<User> foundUser = userRepository.findByEmail(email.toLowerCase());

        if(foundUser.isEmpty()){
            throw new IllegalArgumentException("User not found: " + email);
        }

        User user = foundUser.get();
        user.setEnabled(false);
        userRepository.save(user);

        log.info("User disabled successfully: {}", email);
    }
    
    /**
     * Update user password
     */
    public void updatePassword(String email, String newPassword) {
        log.info("Updating password for user: {}", email);
        
        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase and digit");
        }
        
        Optional<User> foundUser = userRepository.findByEmail(email.toLowerCase());
        
        if(foundUser.isEmpty()){
            throw new IllegalArgumentException("User not found: " + email);
        }
        
        User user = foundUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password updated successfully for user: {}", email);
    }
    
    /**
     * Get total user count
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }
    
    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.info("Finding user by email: {}", email);
        return userRepository.findByEmail(email.toLowerCase());
    }
    
}
