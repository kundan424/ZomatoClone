package com.zomato.clone.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtil
 * =======
 * Utility class responsible for all JWT (JSON Web Token) operations.
 * <p>
 * Responsibilities:
 * 1. Generate signed JWT tokens (JWS)
 * 2. Parse and verify incoming JWT tokens
 * 3. Extract claims (username, expiration, etc.)
 * 4. Validate token integrity and expiration
 * <p>
 * Cryptography:
 * - Algorithm: HS256 (HMAC + SHA-256)
 * - Key type: Symmetric (SecretKey)
 * <p>
 * Token format:
 * header.payload.signature
 */

@Component
public class JwtUtil {

    /**
     * Base64-encoded secret key injected from configuration.
     * <p>
     * Example (application.properties):
     * jwt.secret=Base64Encoded256BitSecret
     * <p>
     * Requirements:
     * - Must be Base64 encoded
     * - Must be at least 256 bits for HS256
     */
    @Value(value = "${jwt.secret}")
    private String secretKey;

    /**
     * Extracts the username (JWT subject) from the token.
     * <p>
     * Internally:
     * - Parses the token
     * - Verifies signature
     * - Reads the "sub" claim
     *
     * @param token JWT string
     *              getSubject() usually represents the username in JWTs
     * @return username stored in token subject
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic claim extraction method.
     * <p>
     * Design rationale:
     * - Centralizes token parsing logic
     * - Avoids repeated parsing code for each claim
     *
     * @param token          JWT string
     * @param claimsResolver function defining which claim to extract
     * @param <T>            expected return type
     * @return extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * Generates a JWT with no custom claims.
     * <p>
     * Typically used when only identity (username/email) is required.
     *
     * @param username authenticated user's username/email
     * @return signed JWT token
     */
    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username);
    }

    /**
     * Generates a signed JWT with optional custom claims.
     * <p>
     * Token creation steps:
     * 1. Attach custom claims (if provided)
     * 2. Set subject (username)
     * 3. Set issued-at timestamp
     * 4. Set expiration timestamp (10 hours)
     * 5. Sign using HS256 and SecretKey
     *
     * @param extraClaims additional claims (roles, userId, etc.)
     * @param username    subject of the token
     * @return compact JWT string
     */
    public String generateToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 Hours validity
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates a JWT token against expected username.
     * <p>
     * Validation rules:
     * 1. Token signature must be valid
     * 2. Token must not be expired
     * 3. Token subject must match provided username
     *
     * @param token    JWT string
     * @param username expected username
     * @return true if token is valid
     */
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }


    /**
     * Checks whether the JWT token has expired.
     *
     * @param token JWT string
     * @return true if expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration timestamp from the token.
     *
     * @param token JWT string
     * @return expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    /**
     * Parses the JWT and returns all claims.
     * <p>
     * Internal behavior:
     * - Verifies signature using verifyWith(SecretKey)
     * - Rejects tampered or invalid tokens
     * - Decodes payload into Claims
     * <p>
     * Uses modern JJWT API:
     * - verifyWith(...) instead of deprecated setSigningKey(...)
     * - parseSignedClaims(...) instead of parseClaimsJws(...)
     *
     * @param token JWT string
     * @return Claims payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Converts Base64-encoded secret into a SecretKey.
     * <p>
     * Reason:
     * - JJWT requires cryptographic Key objects
     * - Prevents weak or incorrectly sized keys
     *
     * @return HMAC-SHA compatible SecretKey
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}