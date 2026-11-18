package com.applyrank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Creates and verifies the login token (JWT) sent in the "Authorization: Bearer ..." header. */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours}") long hours) {
        if (secret.startsWith("change-this-secret"))
            org.slf4j.LoggerFactory.getLogger(JwtService.class).warn("SECURITY: using the default JWT secret. Set the JWT_SECRET environment variable before deploying!");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = hours * 3_600_000L;
    }

    /** Same as below with version 0 (used by tests and for brand new users). */
    public String generate(String email, String role) {
        return generate(email, role, 0);
    }

    /** "version" is the user's tokenVersion at the moment of login. The filter compares it on every request. */
    public String generate(String email, String role, int version) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("v", version)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /** Throws io.jsonwebtoken.JwtException if the token is invalid or expired. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
