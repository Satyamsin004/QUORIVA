package com.csscholar.backend.auth;

import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "super_secret_jwt_test_key_for_unit_tests_that_is_at_least_256_bits_long!",
                900000,
                604800000
        );

        testUser = new User("Jane Doe", "jane@example.com", "hashed_pwd", Role.STUDENT);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should generate and validate access token successfully")
    void shouldGenerateAndValidateAccessToken() {
        String token = tokenProvider.generateAccessToken(testUser);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(testUser.getId(), tokenProvider.getUserIdFromToken(token));
        assertEquals("jane@example.com", tokenProvider.getEmailFromToken(token));
        assertEquals("access", tokenProvider.getTokenType(token));
    }

    @Test
    @DisplayName("Should generate and validate refresh token successfully")
    void shouldGenerateAndValidateRefreshToken() {
        String token = tokenProvider.generateRefreshToken(testUser);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(testUser.getId(), tokenProvider.getUserIdFromToken(token));
        assertEquals("refresh", tokenProvider.getTokenType(token));
    }

    @Test
    @DisplayName("Should correctly hash tokens using SHA-256")
    void shouldHashTokenConsistently() {
        String token = "test-token-123";
        String hash1 = tokenProvider.hashToken(token);
        String hash2 = tokenProvider.hashToken(token);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex is 64 characters
    }

    @Test
    @DisplayName("Should reject invalid or tampered token")
    void shouldRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }
}

