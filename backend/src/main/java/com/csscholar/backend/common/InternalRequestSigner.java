package com.csscholar.backend.common;

import org.slf4j.MDC;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility for generating cryptographically signed internal service requests.
 * Uses HMAC-SHA256 with timestamp replay protection, nonce tracking, deterministic
 * query canonicalization, and body integrity verification.
 */
public final class InternalRequestSigner {

    public static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";
    public static final String HEADER_TIMESTAMP = "X-Internal-Timestamp";
    public static final String HEADER_NONCE = "X-Internal-Nonce";
    public static final String HEADER_SIGNATURE = "X-Internal-Signature";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private InternalRequestSigner() {
    }

    /**
     * Signs and decorates an HttpRequest.Builder with HMAC-SHA256 security headers, nonce, and correlation ID.
     */
    public static HttpRequest.Builder signRequest(
            HttpRequest.Builder builder,
            String method,
            URI uri,
            String body,
            String secretKey
    ) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String bodySha256 = computeSha256Hex(body != null ? body : "");
        String path = uri.getRawPath() != null ? uri.getRawPath() : uri.getPath();
        String canonicalQuery = canonicalizeQuery(uri.getRawQuery() != null ? uri.getRawQuery() : uri.getQuery());

        String canonicalString = method.toUpperCase() + "\n" + path + "\n" + canonicalQuery + "\n" + timestamp + "\n" + nonce + "\n" + bodySha256;
        String signature = computeHmacSha256Hex(canonicalString, secretKey);

        builder.header(HEADER_INTERNAL_KEY, secretKey)
                .header(HEADER_TIMESTAMP, timestamp)
                .header(HEADER_NONCE, nonce)
                .header(HEADER_SIGNATURE, signature);

        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            builder.header(HEADER_CORRELATION_ID, correlationId);
        }

        return builder;
    }

    /**
     * Deterministically sorts query parameters alphabetically by key-value pair.
     */
    public static String canonicalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isBlank())
                .sorted()
                .collect(Collectors.joining("&"));
    }

    public static String computeSha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute SHA-256", e);
        }
    }

    public static String computeHmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
