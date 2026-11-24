package com.hrm.saas.domain.auth.security;

import com.hrm.saas.config.JwtProperties;
import com.hrm.saas.domain.auth.enums.MembershipRole;
import com.hrm.saas.domain.auth.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            // Attempt base64 decode if formatted as base64
            keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        } catch (Exception e) {
            keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an Account-Scoped access token (used before selecting a tenant or for global actions).
     */
    public String generateAccountScopedToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("tokenType", TokenType.ACCOUNT_SCOPED.name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a Tenant-Scoped access token (used for all tenant workspace operations).
     */
    public String generateTenantScopedToken(UUID userId, String email, UUID tenantId, String tenantSlug, MembershipRole role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("tid", tenantId.toString())
                .claim("slug", tenantSlug)
                .claim("role", role.name())
                .claim("tokenType", TokenType.TENANT_SCOPED.name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts Claims from signed token.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public UUID getTenantId(Claims claims) {
        String tid = claims.get("tid", String.class);
        return tid != null ? UUID.fromString(tid) : null;
    }

    public String getTenantSlug(Claims claims) {
        return claims.get("slug", String.class);
    }

    public MembershipRole getRole(Claims claims) {
        String roleStr = claims.get("role", String.class);
        return roleStr != null ? MembershipRole.valueOf(roleStr) : null;
    }

    public TokenType getTokenType(Claims claims) {
        String typeStr = claims.get("tokenType", String.class);
        return typeStr != null ? TokenType.valueOf(typeStr) : null;
    }
}
