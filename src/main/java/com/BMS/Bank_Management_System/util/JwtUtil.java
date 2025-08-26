package com.BMS.Bank_Management_System.util;

import com.BMS.Bank_Management_System.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private Long jwtExpirationMs;

//    @Value("${app.jwt.ai-agent.secret}")
//    private String aiAgentJwtSecret;
//
//    @Value("${app.jwt.ai-agent.expiration}")
//    private Long aiAgentJwtExpirationMs;

    /**
     * Generates a standard JWT for a user, containing only the username.
     * This is used for general application authentication.
     *
     * @param username The username for whom the token is generated.
     * @return A JWT string.
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(jwtSecret))
                .compact();
    }

    /**
     * A convenience method to generate a standard token for a User entity.
     *
     * @param user The user object.
     * @return A JWT string.
     */
    public String generateToken(User user) {
        return generateToken(user.getUsername());
    }

    /**
     * Generates a dedicated JWT for the AI agent with its own claims, secret, and expiration.
     *
     * @param user The user entity representing the AI agent.
     * @return A JWT string with the role "AI_AGENT".
     */
    public String generateAgentToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(jwtSecret))
                .compact();
    }


    /**
     * Validates a JWT by checking its signature and expiration.
     * This method can validate both standard user tokens and AI agent tokens.
     *
     * @param token The JWT to validate.
     * @return True if the token is valid, false otherwise.
     */
    public boolean validateJwtToken(String token) {
        try {
            // The parsing process itself validates the signature.
            // An explicit expiration check is also performed.
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Catches parsing errors, signature mismatches, etc.
            return false;
        }
    }

    /**
     * Extracts the username from a JWT.
     * Works for both standard user and AI agent tokens.
     *
     * @param token The JWT.
     * @return The username (subject) from the token.
     */
    public String getUsernameFromJwtToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * A generic function to extract a specific claim from a token.
     *
     * @param token          The JWT.
     * @param claimsResolver A function to resolve the desired claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationMs, SecretKey secretKey) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        try {
            // Attempt to parse with the standard user secret first.
            return Jwts.parser()
                    .verifyWith(getSigningKey(jwtSecret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            // If it fails, try with the AI agent secret. If this also fails, the exception will be thrown.
            return Jwts.parser()
                    .verifyWith(getSigningKey(jwtSecret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
    }

    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}