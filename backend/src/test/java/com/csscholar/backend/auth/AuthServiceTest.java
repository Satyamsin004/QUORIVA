package com.csscholar.backend.auth;

import com.csscholar.backend.common.DuplicateResourceException;
import com.csscholar.backend.common.UnauthorizedException;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "encoded_password", Role.STUDENT);
        testUser.setId(UUID.randomUUID());

        registerRequest = new RegisterRequest("John Doe", "john@example.com", "Password123!");
        loginRequest = new LoginRequest("john@example.com", "Password123!");
    }

    @Test
    @DisplayName("Registration should succeed when email is not taken")
    void shouldRegisterNewUserSuccessfully() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(testUser.getId());
            return u;
        });
        when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(tokenProvider.hashToken("refresh_token")).thenReturn("hash123");
        when(tokenProvider.getExpirationFromToken("refresh_token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResult response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.rawRefreshToken());
        assertEquals("john@example.com", response.user().getEmail());
        assertEquals(Role.STUDENT.name(), response.user().getRole());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Registration should throw DuplicateResourceException when email already exists")
    void shouldThrowWhenRegisteringExistingEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login should succeed with valid credentials")
    void shouldLoginSuccessfully() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123!", "encoded_password")).thenReturn(true);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("refresh_token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(tokenProvider.hashToken("refresh_token")).thenReturn("hash123");
        when(tokenProvider.getExpirationFromToken("refresh_token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResult response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.rawRefreshToken());
        assertEquals("john@example.com", response.user().getEmail());
    }

    @Test
    @DisplayName("Login should throw BadCredentialsException with invalid password")
    void shouldThrowWithInvalidPassword() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "encoded_password")).thenReturn(false);

        LoginRequest badRequest = new LoginRequest("john@example.com", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(badRequest));
    }

    @Test
    @DisplayName("Token refresh should rotate tokens successfully preserving family ID")
    void shouldRotateTokensOnRefresh() {
        RefreshTokenRequest refreshReq = new RefreshTokenRequest("valid_refresh_token");
        UUID familyId = UUID.randomUUID();
        RefreshToken storedToken = new RefreshToken(testUser, "hash123", Instant.now().plusSeconds(3600), familyId);

        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.getTokenType("valid_refresh_token")).thenReturn("refresh");
        when(tokenProvider.hashToken("valid_refresh_token")).thenReturn("hash123");
        when(refreshTokenRepository.findByTokenHashForUpdate("hash123")).thenReturn(Optional.of(storedToken));

        when(tokenProvider.generateAccessToken(testUser)).thenReturn("new_access_token");
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn("new_refresh_token");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(tokenProvider.hashToken("new_refresh_token")).thenReturn("new_hash123");
        when(tokenProvider.getExpirationFromToken("new_refresh_token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResult response = authService.refreshToken(refreshReq);

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertEquals("new_refresh_token", response.rawRefreshToken());
        assertTrue(storedToken.isRevoked()); // Prior token must be revoked
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    @DisplayName("Token refresh should throw UnauthorizedException and revoke token family when reused token is revoked")
    void shouldThrowWhenRefreshTokenIsRevoked() {
        RefreshTokenRequest refreshReq = new RefreshTokenRequest("revoked_refresh_token");
        UUID familyId = UUID.randomUUID();
        RefreshToken storedToken = new RefreshToken(testUser, "hash123", Instant.now().plusSeconds(3600), familyId);
        storedToken.setRevoked(true);

        when(tokenProvider.validateToken("revoked_refresh_token")).thenReturn(true);
        when(tokenProvider.getTokenType("revoked_refresh_token")).thenReturn("refresh");
        when(tokenProvider.hashToken("revoked_refresh_token")).thenReturn("hash123");
        when(refreshTokenRepository.findByTokenHashForUpdate("hash123")).thenReturn(Optional.of(storedToken));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(refreshReq));
        verify(refreshTokenRepository).revokeAllByFamilyId(familyId);
    }
}
