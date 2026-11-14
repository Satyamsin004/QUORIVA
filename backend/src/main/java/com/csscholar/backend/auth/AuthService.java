package com.csscholar.backend.auth;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.DuplicateResourceException;
import com.csscholar.backend.common.UnauthorizedException;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.user.UserResponse;
import com.csscholar.backend.workspace.CreateWorkspaceRequest;
import com.csscholar.backend.workspace.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final WorkspaceService workspaceService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            WorkspaceService workspaceService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getFullName().trim(),
                normalizedEmail,
                encodedPassword,
                Role.STUDENT
        );

        User savedUser = userRepository.save(user);

        // Auto-create initial personal workspace for the student
        try {
            workspaceService.createWorkspace(
                    savedUser.getId(),
                    new CreateWorkspaceRequest("Personal Workspace", "Default workspace for CS study materials")
            );
        } catch (Exception e) {
            // Log but don't fail registration
            log.warn("Failed to create personal workspace for user {}: {}", savedUser.getId(), e.getMessage());
        }

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser);
        UUID familyId = UUID.randomUUID();

        saveRefreshToken(savedUser, refreshToken, familyId);

        return new AuthResult(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                UserResponse.fromEntity(savedUser)
        );
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is currently disabled. Please contact support.");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        UUID familyId = UUID.randomUUID();

        saveRefreshToken(user, refreshToken, familyId);

        return new AuthResult(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                UserResponse.fromEntity(user)
        );
    }

    @Transactional
    public AuthResult refreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required.");
        }

        if (!tokenProvider.validateToken(rawToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        if (!"refresh".equals(tokenProvider.getTokenType(rawToken))) {
            throw new UnauthorizedException("Provided token is not a valid refresh token.");
        }

        String tokenHash = tokenProvider.hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found or already revoked."));

        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Revoked refresh token reuse detected for user ID: {} (Token ID: {}, Family ID: {})! Invalidation of token family triggered.",
                    storedToken.getUser().getId(), storedToken.getId(), storedToken.getFamilyId());
            boolean isLegacy = storedToken.isLegacy() ||
                    (storedToken.getId() != null && storedToken.getId().equals(storedToken.getFamilyId()));
            if (isLegacy) {
                log.warn("Revoked legacy token replayed without recoverable family: invalidating all tokens for user ID: {}",
                        storedToken.getUser().getId());
                refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            } else if (storedToken.getFamilyId() != null) {
                refreshTokenRepository.revokeAllByFamilyId(storedToken.getFamilyId());
            }
            throw new UnauthorizedException("Refresh token reuse detected. Active session family has been invalidated for security. Please log in again.");
        }

        if (storedToken.isExpired()) {
            throw new UnauthorizedException("Refresh token is expired. Please log in again.");
        }

        // Revoke the used refresh token (Refresh Token Rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is currently disabled.");
        }

        // Generate new token pair; if token was legacy or lacked family, assign a fresh familyId to transition to RFC 6819 family isolation
        boolean isLegacy = storedToken.isLegacy() ||
                (storedToken.getId() != null && storedToken.getId().equals(storedToken.getFamilyId())) ||
                storedToken.getFamilyId() == null;
        UUID familyId = isLegacy ? UUID.randomUUID() : storedToken.getFamilyId();
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken, familyId);

        return new AuthResult(
                newAccessToken,
                newRefreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                UserResponse.fromEntity(user)
        );
    }

    @Transactional
    public AuthResult refreshToken(RefreshTokenRequest request) {
        return refreshToken(request != null ? request.getRefreshToken() : null);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.trim().isEmpty()) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                if (token.getFamilyId() != null) {
                    refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId());
                }
            });
        }
        SecurityContextHolder.clearContext();
    }

    private void saveRefreshToken(User user, String rawRefreshToken, UUID familyId) {
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiresAt = tokenProvider.getExpirationFromToken(rawRefreshToken);

        RefreshToken refreshTokenEntity = new RefreshToken(user, tokenHash, expiresAt, familyId);
        refreshTokenRepository.save(refreshTokenEntity);
    }
}
