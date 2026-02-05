package com.zomato.clone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig defines the main Spring Security configuration
 * for the application.
 *
 * <p>
 * This class configures:
 * <ul>
 *   <li>Which endpoints are public and which require authentication</li>
 *   <li>JWT-based stateless authentication</li>
 *   <li>Custom authentication provider</li>
 *   <li>Security filter ordering</li>
 * </ul>
 *
 * <p>
 * This configuration is designed for REST APIs using JWT
 * (no server-side sessions).
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Custom JWT authentication filter that:
     * <ul>
     *   <li>Extracts JWT from Authorization header</li>
     *   <li>Validates the token</li>
     *   <li>Sets authentication in SecurityContext</li>
     * </ul>
     */
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * AuthenticationProvider responsible for validating
     * username/password credentials during login.
     */
    private final AuthenticationProvider authenticationProvider;


    /**
     * Defines the security filter chain used by Spring Security.
     *
     * <p>
     * This method configures:
     * <ul>
     *   <li>CSRF protection (disabled for stateless APIs)</li>
     *   <li>Authorization rules</li>
     *   <li>Session management strategy</li>
     *   <li>JWT filter placement in the filter chain</li>
     * </ul>
     *
     * @param http HttpSecurity configuration object
     * @return configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                /**
                 * Disable CSRF because:
                 * - We are building a stateless REST API
                 * - Authentication is handled via JWT, not cookies
                 */
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to Auth endpoints and Swagger UI
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // secure everything else
                        .anyRequest().authenticated()
                )
                /**
                 * Configure session management.
                 * STATELESS means:
                 * - Spring Security will not create or use HTTP sessions
                 * - Every request must contain a valid JWT
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)

                /**
                 * Add JWT filter before UsernamePasswordAuthenticationFilter.
                 *
                 * This ensures JWT authentication happens before
                 * Spring attempts username/password authentication.
                 */
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        // Build and return the configured security filter chain
        return http.build();
    }
}

/**
 * Request flow:
 *
 * Request enters Spring Security filter chain
 * JwtAuthenticationFilter runs early
 * JWT is validated (if present)
 * SecurityContext is populated
 * Authorization rules are applied
 * Controller is reached (or blocked)
 */
