package com.csscholar.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for issuing and validating cryptographically signed, session-bound CSRF tokens.
 *
 * Token structure:
 *   Base64URL(salt:sessionId:timestampSeconds:hmacSignature)
 *
 * Security Guarantees:
 * 1. Cryptographic Authenticity: Tokens are signed with HMAC-SHA256 using the server's private key.
 *    Attacker-injected cookies from compromised subdomains or HTTP MITM cannot forge valid signatures.
 * 2. Session Binding: The token binds the specific session/family/user identifier.
 * 3. Expiration: Tokens expire automatically after the configured maximum age (7 days).
 * 4. Timing-Safe Verification: Signature check uses MessageDigest.isEqual to prevent timing attacks.
 */
@Service
public class CsrfTokenService {

    private static final Logger log = LoggerFactory.getLogger(CsrfTokenService.class);
    private static final long MAX_TOKEN_AGE_SECONDS = 7 * 24 * 60 * 60L; // 7 days matching refresh token

    private final byte[] secretKeyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public CsrfTokenService(
            @Value("${security.csrf.secret:${jwt.secret:cs_scholar_csrf_default_secure_secret_key_minimum_256_bits}}") String secretKey
    ) {
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a signed, session-bound CSRF token.
     *
     * @param sessionIdentifier The session ID, family ID, or user ID to bind.
     * @return Base64URL-encoded signed CSRF token.
     */
    public String generateToken(String sessionIdentifier) {
        String sessionId = sessionIdentifier != null ? sessionIdentifier : "anon";
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);
        long timestamp = Instant.now().getEpochSecond();

        String payload = salt + ":" + sessionId + ":" + timestamp;
        String signature = computeHmac(payload);

        String fullToken = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fullToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates that the provided CSRF token has a valid cryptographic signature,
     * has not expired, and optionally matches the expected session identifier.
     *
     * @param token              The Base64URL-encoded token to validate.
     * @param expectedSessionId  Optional expected session identifier to verify binding.
     * @return true if the token is authentic, valid, and unexpired.
     */
    public boolean validateToken(String token, String expectedSessionId) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token.trim());
            String fullToken = new String(decodedBytes, StandardCharsets.UTF_8);

            String[] parts = fullToken.split(":");
            if (parts.length != 4) {
                log.warn("CSRF token rejected: Malformed token structure (expected 4 parts, got {})", parts.length);
                return false;
            }

            String salt = parts[0];
            String tokenSessionId = parts[1];
            long timestamp = Long.parseLong(parts[2]);
            String providedSignature = parts[3];

            // 1. Expiration check
            long now = Instant.now().getEpochSecond();
            if (now - timestamp > MAX_TOKEN_AGE_SECONDS || timestamp > now + 300) {
                log.warn("CSRF token rejected: Expired or skewed timestamp (age: {}s)", now - timestamp);
                return false;
            }

            // 2. Cryptographic signature verification
            String payload = salt + ":" + tokenSessionId + ":" + timestamp;
            String expectedSignature = computeHmac(payload);

            byte[] expectedSigBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] providedSigBytes = providedSignature.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedSigBytes, providedSigBytes)) {
                log.warn("CSRF token rejected: Invalid or forged cryptographic signature.");
                return false;
            }

            // 3. Optional session binding verification
            if (expectedSessionId != null && !expectedSessionId.isBlank()) {
                if (!expectedSessionId.equals(tokenSessionId)) {
                    log.warn("CSRF token rejected: Token session ID '{}' does not match expected '{}'",
                            tokenSessionId, expectedSessionId);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("CSRF token validation failed with exception: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate CSRF HMAC signature", e);
        }
    }
}

