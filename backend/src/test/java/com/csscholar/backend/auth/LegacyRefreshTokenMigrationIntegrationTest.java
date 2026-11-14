package com.csscholar.backend.auth;

import com.csscholar.backend.common.UnauthorizedException;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class LegacyRefreshTokenMigrationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("legacy_test_" + UUID.randomUUID() + "@university.edu");
        testUser.setFullName("Legacy Test Scholar");
        testUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456789012345678901234567890");
        testUser.setRole(Role.STUDENT);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Pre-migration revoked token replay must revoke active successor and prevent silent authorization")
    @Transactional
    void shouldRevokeActiveSuccessorWhenLegacyRevokedTokenIsReplayed() {
        // Given: Simulate pre-migration state where historical parent/child relationship is absent
        // T1 was revoked prior to migration (family_id = T1.id, is_legacy = true)
        String rawToken1 = tokenProvider.generateRefreshToken(testUser);
        RefreshToken legacyRevokedToken = new RefreshToken(
                testUser,
                tokenProvider.hashToken(rawToken1),
                Instant.now().plusSeconds(86400)
        );
        legacyRevokedToken.setRevoked(true);
        legacyRevokedToken.setLegacy(true);
        legacyRevokedToken = refreshTokenRepository.saveAndFlush(legacyRevokedToken);
        legacyRevokedToken.setFamilyId(legacyRevokedToken.getId()); // emulate V14 family_id = id backfill
        legacyRevokedToken = refreshTokenRepository.saveAndFlush(legacyRevokedToken);

        // T2 is the active successor created before migration (family_id = T2.id, is_legacy = true)
        String rawToken2 = tokenProvider.generateRefreshToken(testUser);
        RefreshToken legacyActiveSuccessor = new RefreshToken(
                testUser,
                tokenProvider.hashToken(rawToken2),
                Instant.now().plusSeconds(86400)
        );
        legacyActiveSuccessor.setRevoked(false);
        legacyActiveSuccessor.setLegacy(true);
        legacyActiveSuccessor = refreshTokenRepository.saveAndFlush(legacyActiveSuccessor);
        legacyActiveSuccessor.setFamilyId(legacyActiveSuccessor.getId()); // emulate V14 family_id = id backfill
        legacyActiveSuccessor = refreshTokenRepository.saveAndFlush(legacyActiveSuccessor);

        // When: Attacker attempts to replay legacy revoked token T1
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authService.refreshToken(rawToken1)
        );
        assertTrue(ex.getMessage().contains("Refresh token reuse detected"));

        // Then: The active successor T2 must NOT remain valid!
        RefreshToken refreshedT2 = refreshTokenRepository.findById(legacyActiveSuccessor.getId()).orElseThrow();
        assertTrue(refreshedT2.isRevoked(), "Active successor must be revoked following legacy replay");

        // Verifying subsequent use of T2 fails with 401
        assertThrows(UnauthorizedException.class, () ->
                authService.refreshToken(rawToken2)
        );
    }

    @Test
    @DisplayName("Active legacy token rotation transitions session into a modern RFC 6819 token family")
    @Transactional
    void shouldTransitionActiveLegacyTokenToNewFamilyOnSuccessfulRotation() {
        // Given: An active legacy token
        String rawTokenA = tokenProvider.generateRefreshToken(testUser);
        RefreshToken legacyActiveToken = new RefreshToken(
                testUser,
                tokenProvider.hashToken(rawTokenA),
                Instant.now().plusSeconds(86400)
        );
        legacyActiveToken.setRevoked(false);
        legacyActiveToken.setLegacy(true);
        legacyActiveToken = refreshTokenRepository.saveAndFlush(legacyActiveToken);
        legacyActiveToken.setFamilyId(legacyActiveToken.getId());
        legacyActiveToken = refreshTokenRepository.saveAndFlush(legacyActiveToken);

        // When: Legitimate user refreshes token
        AuthResult result = authService.refreshToken(rawTokenA);
        assertNotNull(result);
        assertNotNull(result.rawRefreshToken());

        // Then: The newly created token has a distinct family_id (not equal to legacy token id)
        RefreshToken newTokenEntity = refreshTokenRepository.findByTokenHash(tokenProvider.hashToken(result.rawRefreshToken()))
                .orElseThrow();
        assertNotEquals(legacyActiveToken.getId(), newTokenEntity.getFamilyId(),
                "New token must belong to a newly created token family");

        // The legacy token is marked revoked
        RefreshToken originalToken = refreshTokenRepository.findById(legacyActiveToken.getId()).orElseThrow();
        assertTrue(originalToken.isRevoked());
    }
}

