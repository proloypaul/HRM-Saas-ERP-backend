package com.hrm.saas.domain.auth.security;

import com.hrm.saas.config.JwtProperties;
import com.hrm.saas.domain.auth.enums.TokenType;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider — Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(Duration.ofDays(7));
        jwtProperties.setIssuer("hrm-saas");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate and validate valid Account-Scoped token")
    void shouldGenerateAndValidateAccountScopedToken() {
        UUID userId = UUID.randomUUID();
        String email = "admin@example.com";

        String token = jwtTokenProvider.generateAccountScopedToken(userId, email);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertEquals(userId, jwtTokenProvider.getUserId(claims));
        assertEquals(email, jwtTokenProvider.getEmail(claims));
        assertEquals(TokenType.ACCOUNT_SCOPED, jwtTokenProvider.getTokenType(claims));
        assertNull(jwtTokenProvider.getTenantId(claims));
    }

    @Test
    @DisplayName("Should generate and validate valid Tenant-Scoped token")
    void shouldGenerateAndValidateTenantScopedToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "hr@acme.com";
        String slug = "acme";
        Boolean isOwner = false;
        java.util.List<String> roles = java.util.List.of("TENANT_ADMIN");

        String token = jwtTokenProvider.generateTenantScopedToken(userId, email, tenantId, slug, isOwner, roles);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertEquals(userId, jwtTokenProvider.getUserId(claims));
        assertEquals(email, jwtTokenProvider.getEmail(claims));
        assertEquals(tenantId, jwtTokenProvider.getTenantId(claims));
        assertEquals(slug, jwtTokenProvider.getTenantSlug(claims));
        assertEquals(isOwner, jwtTokenProvider.getIsOwner(claims));
        assertEquals(roles, jwtTokenProvider.getRoles(claims));
        assertEquals(TokenType.TENANT_SCOPED, jwtTokenProvider.getTokenType(claims));
    }

    @Test
    @DisplayName("Should reject tampered or invalid token")
    void shouldRejectInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.structure"));
        assertFalse(jwtTokenProvider.validateToken(""));
    }
}
