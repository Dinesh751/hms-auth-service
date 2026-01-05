package com.hms.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hms.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    // find user by email
    Optional<User> findByEmail(String email);

    // check if user exists by email
    boolean existsByEmail(String email);

    // find enabled user by email
    @Query("SELECT u from User u WHERE u.email = :email AND u.enabled = true")
    Optional<User> findByEmailAndEnabled(String email);

}
