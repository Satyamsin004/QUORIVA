package com.csscholar.backend;

import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.document.dto.UploadUrlRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CleanStateVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String testUserEmail;

    @AfterEach
    void tearDown() {
        if (testUserEmail != null) {
            jdbcTemplate.update("DELETE FROM documents WHERE owner_id IN (SELECT id FROM users WHERE email = ?)", testUserEmail);
            jdbcTemplate.update("DELETE FROM workspace_members WHERE user_id IN (SELECT id FROM users WHERE email = ?)", testUserEmail);
            jdbcTemplate.update("DELETE FROM workspaces WHERE owner_id IN (SELECT id FROM users WHERE email = ?)", testUserEmail);
            jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email = ?)", testUserEmail);
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", testUserEmail);
        }
    }

    @Test
    @DisplayName("Verify fresh registration, clean empty state, and document upload initialization")
    void shouldVerifyCleanStateExperienceForNewUser() throws Exception {
        // 1. Fresh user registration
        testUserEmail = "brand_new_user_" + UUID.randomUUID() + "@example.com";
        String uniqueEmail = testUserEmail;
        RegisterRequest registerReq = new RegisterRequest("New Scholar", uniqueEmail, "SecurePass123!");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email").value(uniqueEmail))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.path("data").path("accessToken").asText();
        assertNotNull(token);

        // 2. Fetch user's workspaces — brand new user should have their personal default workspace
        MvcResult workspaceResult = mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn();

        JsonNode workspaceJson = objectMapper.readTree(workspaceResult.getResponse().getContentAsString());
        String workspaceIdStr = workspaceJson.path("data").get(0).path("id").asText();
        UUID workspaceId = UUID.fromString(workspaceIdStr);

        // 3. Verify brand new workspace has ZERO documents
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 4. Verify brand new workspace has ZERO conversations
        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 5. Verify user has ZERO quizzes
        mockMvc.perform(get("/api/v1/learning/quizzes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 6. Verify user has ZERO flashcard decks
        mockMvc.perform(get("/api/v1/learning/decks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 7. Verify user can initiate document upload and normal ingestion flow
        UploadUrlRequest uploadReq = new UploadUrlRequest(
                workspaceId,
                "computer_systems_lecture.pdf",
                "application/pdf",
                1024L * 1024L,
                null,
                "computer_systems_lecture.pdf",
                "Initial lecture notes for CS Systems"
        );

        mockMvc.perform(post("/api/v1/documents/upload-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId", notNullValue()))
                .andExpect(jsonPath("$.data.uploadUrl", notNullValue()));

        // 8. Confirm the document was created with PENDING_UPLOAD status
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("computer_systems_lecture.pdf"))
                .andExpect(jsonPath("$.data.content[0].processingStatus").value("PENDING_UPLOAD"));
    }
}

