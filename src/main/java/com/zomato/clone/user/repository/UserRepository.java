package com.zomato.clone.user.repository;

import com.zomato.clone.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email (Critical for Login/Auth)
    Optional<User> findByEmail(String email);


    // Check if email exists before registering (Validation)
    boolean existsByEmail(String email);
}
