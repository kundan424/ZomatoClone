package com.zomato.clone.user.service;

import com.zomato.clone.config.JwtUtil;
import com.zomato.clone.enums.UserRole;
import com.zomato.clone.user.dto.AuthResponse;
import com.zomato.clone.user.dto.LoginRequest;
import com.zomato.clone.user.dto.RegisterRequest;
import com.zomato.clone.user.entity.User;
import com.zomato.clone.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>
 * These tests validate authentication-related business logic,
 * including:
 * <ul>
 *   <li>User registration flow</li>
 *   <li>User login flow</li>
 * </ul>
 *
 * <p>
 * External dependencies such as repositories, password encoder,
 * JWT utility, and authentication manager are mocked using Mockito.
 *
 * <p>
 * This ensures tests focus only on AuthService logic without
 * requiring database or security infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    /**
     * Mock repository used to simulate database interactions.
     */
    @Mock
    private UserRepository userRepository;

    /**
     * Mock password encoder used to simulate password hashing.
     */
    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * Mock JWT utility used to simulate token generation.
     */
    @Mock
    private JwtUtil jwtUtil;

    /**
     * Mock authentication manager used during login authentication.
     */
    @Mock
    private AuthenticationManager authenticationManager;

    /**
     * AuthService instance under test.
     * Mockito injects mocked dependencies automatically.
     */
    @InjectMocks
    private AuthService authService;


    // ================= REGISTER TESTS =================

    /**
     * Verifies successful registration when valid request is provided.
     *
     * <p>
     * Expected behaviour:
     * <ul>
     *   <li>Email must not already exist</li>
     *   <li>Password must be encoded</li>
     *   <li>User must be saved</li>
     *   <li>JWT token must be generated and returned</li>
     * </ul>
     */
    @Test
    void register_ShouldReturnToken_WhenValidRequest() {

        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .email("john@test.com")
                .password("1234")
                .phone("9999999999")
                .role(UserRole.valueOf("USER"))
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(request.getEmail())).thenReturn("mockToken");

        AuthResponse response = authService.register(request);
        System.out.println(response);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());

        // Verify user was saved in repository
        verify(userRepository).save(any(User.class));
    }

    /**
     * Verifies registration fails when email already exists.
     *
     * <p>
     * Expected behaviour:
     * <ul>
     *   <li>Service should throw RuntimeException</li>
     *   <li>No user should be saved</li>
     * </ul>
     */
    @Test
    void register_ShouldThrowException_WhenEmailExists() {

        RegisterRequest request = RegisterRequest.builder()
                .email("john@test.com")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertEquals("Email already in use", exception.getMessage());
    }


    // ================= LOGIN TESTS =================

    /**
     * Verifies successful login when credentials are valid.
     *
     * <p>
     * Expected behaviour:
     * <ul>
     *   <li>Authentication manager should validate credentials</li>
     *   <li>User should be fetched from repository</li>
     *   <li>JWT token should be generated</li>
     * </ul>
     */
    @Test
    void login_ShouldReturnToken_WhenValidCredentials() {

        LoginRequest request = LoginRequest.builder()
                .email("john@test.com")
                .password("1234")
                .build();

        User user = User.builder()
                .email("john@test.com")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(jwtUtil.generateToken(user.getEmail()))
                .thenReturn("mockToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());

        // Verify authentication manager was called
        verify(authenticationManager).authenticate(any());
    }

    /**
     * Verifies login fails when user does not exist.
     *
     * <p>
     * Expected behaviour:
     * <ul>
     *   <li>Repository returns empty result</li>
     *   <li>Service throws exception</li>
     * </ul>
     */
    @Test
    void login_ShouldThrowException_WhenUserNotFound() {

        LoginRequest request = LoginRequest.builder()
                .email("notfound@test.com")
                .password("1234")
                .build();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.login(request));
    }
}
