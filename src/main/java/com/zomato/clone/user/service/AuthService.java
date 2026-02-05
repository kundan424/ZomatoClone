package com.zomato.clone.user.service;

import com.zomato.clone.config.JwtUtil;
import com.zomato.clone.user.dto.AuthResponse;
import com.zomato.clone.user.dto.LoginRequest;
import com.zomato.clone.user.dto.RegisterRequest;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService handles authentication-related business logic
 * such as user registration and (optionally) login.
 *
 * <p>
 * This service is responsible for:
 * <ul>
 *   <li>Registering new users</li>
 *   <li>Encrypting user passwords</li>
 *   <li>Generating JWT tokens</li>
 * </ul>
 *
 * <p>
 * Note: AuthenticationManager is injected for future login support.
 */

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * AuthenticationManager used for authenticating
     * username/password during login (not used in register).
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user in the system.
     *
     * <p>
     * Registration flow:
     * <ol>
     *   <li>Check if the email is already registered</li>
     *   <li>Encrypt the user's password</li>
     *   <li>Create and save a new User entity</li>
     *   <li>Generate a JWT token for the newly registered user</li>
     * </ol>
     *
     * <p>
     * If registration is successful, a JWT token is returned
     * so the user can access protected endpoints immediately.
     *
     * @param request contains registration details such as
     *                name, email, password, phone, and role
     * @return AuthResponse containing the generated JWT token
     * @throws RuntimeException if email is already in use
     */

    public AuthResponse register(RegisterRequest request) {

        // 1. if user exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // 2. Build User Entity
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        userRepository.save(user);

        var jwtToken = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder().token(jwtToken).build();
    }


    /**
     * Authenticates an existing user and generates a JWT token.
     *
     * <p>
     * Login flow:
     * <ol>
     *   <li>Authenticate user credentials using AuthenticationManager</li>
     *   <li>Load user details from the database</li>
     *   <li>Generate a JWT token for the authenticated user</li>
     * </ol>
     *
     * <p>
     * If authentication fails, Spring Security automatically throws
     * an AuthenticationException.
     *
     * @param request contains login credentials (email and password)
     * @return AuthResponse containing generated JWT token
     */
    public AuthResponse login(LoginRequest request) {
        //1. Authenticate (Checks email & password)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. if successful generates token
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var jwtToken = jwtUtil.generateToken(user.getEmail());
        return AuthResponse.builder().token(jwtToken).build();
    }

}
