package com.csscholar.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register should return tokens, set HttpOnly cookie, omit refreshToken from JSON body, and set XSRF cookie")
    void shouldRegisterUser() throws Exception {
        String uniqueEmail = "student_" + UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest("Test Student", uniqueEmail, "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist()) // Must NOT be in response JSON
                .andExpect(jsonPath("$.data.user.email").value(uniqueEmail))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(cookie().path("XSRF-TOKEN", "/"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email should return 409 Conflict")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        String uniqueEmail = "dup_" + UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest("Test Student", uniqueEmail, "SecurePassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should authenticate, set cookies, and omit refreshToken from JSON")
    void shouldLoginSuccessfully() throws Exception {
        String uniqueEmail = "login_" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest("Login Student", uniqueEmail, "StrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(uniqueEmail, "StrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password should return 401 Unauthorized")
    void shouldFailLoginWithBadPassword() throws Exception {
        String uniqueEmail = "badpwd_" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest("Bad Pwd", uniqueEmail, "CorrectPassword123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(uniqueEmail, "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without token should return 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh with HttpOnly cookie requires CSRF Double-Submit token")
    void shouldRequireCsrfForCookieRefresh() throws Exception {
        String email = "refresh_csrf_" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest("CSRF Student", email, "SecurePassword123!");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie refreshCookie = regResult.getResponse().getCookie(AuthController.REFRESH_COOKIE_NAME);
        Cookie xsrfCookie = regResult.getResponse().getCookie(AuthController.XSRF_COOKIE_NAME);
        assertNotNull(refreshCookie);
        assertNotNull(xsrfCookie);

        // 1. Refresh attempt with refresh cookie but WITHOUT CSRF header must fail with 403 Forbidden
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("CSRF token")));

        // 2. Refresh attempt with MISMATCHED CSRF header must fail with 403 Forbidden
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie)
                        .header(AuthController.XSRF_HEADER_NAME, "wrong-csrf-token"))
                .andExpect(status().isForbidden());

        // 3. Refresh attempt with matching CSRF header succeeds with 200 OK
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie)
                        .header(AuthController.XSRF_HEADER_NAME, xsrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout should clear cookies and revoke refresh token")
    void shouldRevokeRefreshTokenAndClearCookiesOnLogout() throws Exception {
        String email = "logout_" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest("Logout Student", email, "SecurePassword123!");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie refreshCookie = regResult.getResponse().getCookie(AuthController.REFRESH_COOKIE_NAME);
        Cookie xsrfCookie = regResult.getResponse().getCookie(AuthController.XSRF_COOKIE_NAME);

        // Logout with cookies and CSRF header
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(refreshCookie, xsrfCookie)
                        .header(AuthController.XSRF_HEADER_NAME, xsrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));

        // Subsequent refresh with the revoked cookie must fail with 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie)
                        .header(AuthController.XSRF_HEADER_NAME, xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }
}
