package com.hrm.saas.domain.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenHashUtils — Unit Tests")
class TokenHashUtilsTest {

    @Test
    @DisplayName("Should hash token to 64-character SHA-256 hex string")
    void shouldHashTokenToSha256Hex() {
        String rawToken = "sample-secret-token-12345";
        String hash1 = TokenHashUtils.hashToken(rawToken);
        String hash2 = TokenHashUtils.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2, "Hashing must be deterministic");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on blank or null token")
    void shouldThrowOnBlankOrNull() {
        assertThrows(IllegalArgumentException.class, () -> TokenHashUtils.hashToken(null));
        assertThrows(IllegalArgumentException.class, () -> TokenHashUtils.hashToken("   "));
    }
}
