package com.safeme.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${safeme.jwt.secret:}") String configuredSecret,
            @Value("${safeme.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
        byte[] keyBytes;
        if (configuredSecret != null && configuredSecret.trim().getBytes(StandardCharsets.UTF_8).length >= 32) {
            keyBytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
        } else {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET environment variable not set; generated an ephemeral in-memory 256-bit HMAC signing key.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject() != null && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
