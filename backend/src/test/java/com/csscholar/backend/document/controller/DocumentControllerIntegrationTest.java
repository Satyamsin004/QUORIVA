package com.csscholar.backend.document.controller;

import com.csscholar.backend.auth.AuthResponse;
import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.document.dto.DocumentResponse;
import com.csscholar.backend.document.dto.UploadUrlRequest;
import com.csscholar.backend.document.dto.UploadUrlResponse;
import com.csscholar.backend.workspace.CreateWorkspaceRequest;
import com.csscholar.backend.workspace.WorkspaceResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String obtainAccessToken(String email) throws Exception {
        RegisterRequest register = new RegisterRequest("Test User", email, "Password123!");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        return response.getData().getAccessToken();
    }

    private UUID createWorkspace(String token, String name) throws Exception {
        CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest(name, "Workspace description");
        MvcResult wsResult = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<WorkspaceResponse> wsResponse = objectMapper.readValue(
                wsResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        return wsResponse.getData().getId();
    }

    @Test
    @DisplayName("POST /api/v1/documents/upload-url should return presigned URL for authenticated user")
    void shouldGenerateUploadUrl() throws Exception {
        String token = obtainAccessToken("doc_test_" + UUID.randomUUID() + "@example.com");
        UUID workspaceId = createWorkspace(token, "Operating Systems");

        UploadUrlRequest request = new UploadUrlRequest(
                workspaceId,
                "syllabus.pdf",
                "application/pdf",
                1048576L,
                null,
                "OS Syllabus",
                "Course syllabus for OS"
        );

        mockMvc.perform(post("/api/v1/documents/upload-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId", notNullValue()))
                .andExpect(jsonPath("$.data.uploadUrl", notNullValue()))
                .andExpect(jsonPath("$.data.storageKey", notNullValue()))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/documents/upload-url unauthenticated should return 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedUpload() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest(
                UUID.randomUUID(),
                "test.pdf",
                "application/pdf",
                1024L,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/documents/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/documents should return paginated list of accessible documents")
    void shouldListDocuments() throws Exception {
        String token = obtainAccessToken("list_test_" + UUID.randomUUID() + "@example.com");
        UUID workspaceId = createWorkspace(token, "Algorithms");

        // create a document upload request
        UploadUrlRequest uploadReq = new UploadUrlRequest(
                workspaceId,
                "sorting.pdf",
                "application/pdf",
                512000L,
                null,
                "Sorting Algorithms",
                "Sorting notes"
        );

        mockMvc.perform(post("/api/v1/documents/upload-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + token)
                        .param("workspaceId", workspaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @DisplayName("GET and DELETE /api/v1/documents/{id} document lifecycle")
    void shouldGetAndDeleteDocument() throws Exception {
        String token = obtainAccessToken("lifecycle_" + UUID.randomUUID() + "@example.com");
        UUID workspaceId = createWorkspace(token, "Databases");

        UploadUrlRequest uploadReq = new UploadUrlRequest(
                workspaceId,
                "relational_algebra.pdf",
                "application/pdf",
                256000L,
                null,
                "Relational Algebra",
                "Notes"
        );

        MvcResult uploadResult = mockMvc.perform(post("/api/v1/documents/upload-url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<UploadUrlResponse> uploadResponse = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        UUID documentId = uploadResponse.getData().getDocumentId();

        // Get details
        mockMvc.perform(get("/api/v1/documents/" + documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(documentId.toString()))
                .andExpect(jsonPath("$.data.title").value("Relational Algebra"))
                .andExpect(jsonPath("$.data.processingStatus").value("PENDING_UPLOAD"));

        // Delete document
        mockMvc.perform(delete("/api/v1/documents/" + documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify it is no longer found (soft deleted)
        mockMvc.perform(get("/api/v1/documents/" + documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
