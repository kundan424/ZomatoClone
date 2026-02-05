package com.zomato.clone.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter is a custom Spring Security filter that runs
 * once per HTTP request.
 * <p>
 * <p>
 * Its responsibility is to:
 * <p>
 * Extract JWT token from the Authorization header
 * Validate the token
 * Authenticate the user and set authentication in SecurityContext
 * <p>
 * This filter ensures that protected endpoints are accessed only by
 * authenticated users.
 */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Utility class responsible for JWT operations
     * (extract username, validate token, etc.)
     */
    private final JwtUtil jwtUtil;

    /**
     * Spring Security service used to load user details
     * from database or another source.
     */
    private final UserDetailsService userDetailsService;

    /**
     * This method is executed for every incoming HTTP request.
     *
     * @param request     incoming HTTP request
     * @param response    outgoing HTTP response
     * @param filterChain chain of security filters
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Read Authorization header from the request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        /**
         * Step 1: Check if Authorization header exists
         * and starts with "Bearer ".
         * If not, skip authentication and continue the filter chain.
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * Step 2: Extract JWT token from header.
         * Example:
         * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
         */
        jwt = authHeader.substring(7);
        userEmail = jwtUtil.extractUsername(jwt);

        /**
         * Step 3: Authenticate user only if:
         * - Token contains a valid username
         * - User is not already authenticated in the SecurityContext
         */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user details from database or user service
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 4. Validate token , If valid, create Authentication object.
            if (jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {

                // Create authentication token with user authorities (roles)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Attach request-specific details (IP, session, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                /**
                 * Step 5: Store authentication in SecurityContext.
                 * After this, Spring Security considers the user authenticated.
                 */
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
            }
        }

        // continue the remaining filter chain
        filterChain.doFilter(request, response);
    }
}