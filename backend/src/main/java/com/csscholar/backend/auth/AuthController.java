package com.csscholar.backend.auth;

import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.UnauthorizedException;
import com.csscholar.backend.user.UserResponse;
import com.csscholar.backend.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    public static final String XSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String XSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final int REFRESH_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7 days

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    private final AuthService authService;
    private final UserService userService;
    private final CsrfTokenService csrfTokenService;

    public AuthController(AuthService authService, UserService userService, CsrfTokenService csrfTokenService) {
        this.authService = authService;
        this.userService = userService;
        this.csrfTokenService = csrfTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResult authResult = authService.register(request);
        setRefreshTokenCookie(response, authResult.rawRefreshToken(), REFRESH_COOKIE_MAX_AGE_SECONDS);
        setCsrfCookie(response, authResult.user().getId().toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully.", authResult.toResponse()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult authResult = authService.login(request);
        setRefreshTokenCookie(response, authResult.rawRefreshToken(), REFRESH_COOKIE_MAX_AGE_SECONDS);
        setCsrfCookie(response, authResult.user().getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authResult.toResponse()));
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String cookieToken = extractCookie(httpRequest, REFRESH_COOKIE_NAME);
        boolean fromCookie = cookieToken != null && !cookieToken.isBlank();
        String token = fromCookie ? cookieToken : (request != null ? request.getRefreshToken() : null);

        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Refresh token is required.");
        }

        if (fromCookie) {
            validateCsrf(httpRequest);
        }

        AuthResult authResult = authService.refreshToken(token);
        setRefreshTokenCookie(httpResponse, authResult.rawRefreshToken(), REFRESH_COOKIE_MAX_AGE_SECONDS);
        setCsrfCookie(httpResponse, authResult.user().getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed successfully.", authResult.toResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String cookieToken = extractCookie(httpRequest, REFRESH_COOKIE_NAME);
        boolean fromCookie = cookieToken != null && !cookieToken.isBlank();
        String token = fromCookie ? cookieToken : (request != null ? request.getRefreshToken() : null);

        if (fromCookie) {
            validateCsrf(httpRequest);
        }

        authService.logout(token);
        clearRefreshTokenCookie(httpResponse);
        clearCsrfCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Full authentication is required to access your profile.");
        }
        UserResponse response = userService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved.", response));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setCsrfCookie(HttpServletResponse response, String sessionIdentifier) {
        String csrfToken = csrfTokenService.generateToken(sessionIdentifier);
        ResponseCookie cookie = ResponseCookie.from(XSRF_COOKIE_NAME, csrfToken)
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .maxAge(REFRESH_COOKIE_MAX_AGE_SECONDS)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCsrfCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(XSRF_COOKIE_NAME, "")
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void validateCsrf(HttpServletRequest request) {
        String csrfCookie = extractCookie(request, XSRF_COOKIE_NAME);
        String csrfHeader = request.getHeader(XSRF_HEADER_NAME);

        if (csrfCookie == null || csrfCookie.isBlank() || csrfHeader == null || csrfHeader.isBlank() || !csrfCookie.equals(csrfHeader)) {
            log.warn("CSRF double-submit validation failed for auth request. Cookie: {}, Header: {}",
                    csrfCookie != null ? "present" : "missing",
                    csrfHeader != null ? "present" : "missing");
            throw new ForbiddenException("Invalid or missing CSRF token.");
        }

        // Cryptographically verify signature and expiration
        if (!csrfTokenService.validateToken(csrfHeader, null)) {
            log.warn("CSRF cryptographic token validation failed (forged, tampered, or expired token).");
            throw new ForbiddenException("Invalid or forged CSRF token.");
        }
    }
}
