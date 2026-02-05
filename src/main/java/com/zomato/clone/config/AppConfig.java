package com.zomato.clone.config;

import com.zomato.clone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AppConfig contains core authentication-related bean definitions
 * used by Spring Security.
 *
 * <p>
 * <h3>This configuration:</h3>
 * <ul>
 *   <li>Defines how users are loaded from the database</li>
 *   <li>Configures username/password authentication</li>
 *   <li>Provides password hashing strategy</li>
 *   <li>Exposes AuthenticationManager for login flows</li>
 * </ul>
 *
 * <p>
 * This setup is compatible with Spring Boot 3 and Spring Security 6.
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    // Repository used to fetch user data from the database.
    private final UserRepository userRepository;


    /**
     * UserDetailsService is used by Spring Security to load user-specific
     * data during authentication.
     *
     * <p>
     * In this implementation:
     * <ul>
     *   <li>User is fetched using email (used as username)</li>
     *   <li>Password is read as-is (must already be BCrypt-encoded)</li>
     *   <li>User role is mapped to Spring Security roles</li>
     * </ul>
     *
     * @return UserDetailsService implementation backed by database
     */

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name()) // Sets role as ROLE_USER, etc.
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * AuthenticationProvider responsible for validating
     * username and password during login.
     *
     * <p>
     * DaoAuthenticationProvider:
     * <ul>
     *   <li>Uses UserDetailsService to load user information</li>
     *   <li>Uses PasswordEncoder to verify hashed passwords</li>
     * </ul>
     *
     * <p>
     * Spring Security 6 requires UserDetailsService to be
     * provided via constructor injection.
     *
     * @param userDetailsService injected UserDetailsService bean
     * @return configured AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);

        // Configure password hashing strategy
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }



    /**
     * AuthenticationManager is the main entry point for authentication.
     *
     * <p>
     * It delegates authentication requests to the configured
     * AuthenticationProvider(s).
     *
     * <p>
     * This bean is commonly used in login controllers
     * to authenticate username/password credentials.
     *
     * @param config Spring Security authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}