package com.csscholar.backend.auth;

import com.csscholar.backend.user.UserResponse;

public record AuthResult(
        String accessToken,
        String rawRefreshToken,
        long expiresInMs,
        UserResponse user
) {
    public AuthResponse toResponse() {
        return new AuthResponse(accessToken, expiresInMs, user);
    }
}

