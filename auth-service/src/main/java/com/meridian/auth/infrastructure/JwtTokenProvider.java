package com.meridian.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class responsible for the cryptographic generation and validation
 * of JSON Web Tokens (JWT).
 * <p>
 * Uses the {@code jjwt} library. Secures tokens using HMAC SHA-256 with a
 * symmetric secret key explicitly injected from application configuration.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Generates a new short-lived JWT Access Token for an authenticated user.
     * <p>
     * The token's subject is the username, and it includes the user's granted
     * authorities
     * (roles) as a custom claim.
     *
     * @param authentication The Spring {@link Authentication} object containing the
     *                       principal's details.
     * @return A compactly serialized, Base64-URL encoded, cryptographically signed
     *         JWT string.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parses a JWT to extract the subject (username).
     * <p>
     * <b>Note:</b> This method cryptographically verifies the signature during
     * parsing.
     * If the token is tampered with or expired, it will throw a JwtException.
     *
     * @param token The raw JWT string.
     * @return The subject (username) encoded within the token.
     */
    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Cryptographically validates the integrity and expiration of a JWT.
     *
     * @param authToken The raw JWT string to validate.
     * @return {@code true} if the token is valid, unaltered, and unexpired;
     *         {@code false} otherwise.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (Exception ex) {
            // Log exceptions depending on token issues
            return false;
        }
    }
}
