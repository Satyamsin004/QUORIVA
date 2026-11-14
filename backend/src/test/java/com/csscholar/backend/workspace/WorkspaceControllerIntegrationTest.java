package com.csscholar.backend.workspace;

import com.csscholar.backend.auth.AuthResponse;
import com.csscholar.backend.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register primary user
        String email1 = "alice_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req1 = new RegisterRequest("Alice Researcher", email1, "Password123!");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        userToken = root1.get("data").get("accessToken").asText();

        // Register other user
        String email2 = "bob_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req2 = new RegisterRequest("Bob Student", email2, "Password123!");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        otherUserToken = root2.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Create, Read, Update, Delete workspace lifecycle")
    void shouldManageWorkspaceLifecycle() throws Exception {
        // 1. Create Workspace
        CreateWorkspaceRequest createReq = new CreateWorkspaceRequest("CS189 - Machine Learning", "Lecture notes and papers");
        MvcResult createResult = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("CS189 - Machine Learning"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String workspaceId = createdJson.get("data").get("id").asText();

        // 2. Get Workspace by ID
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(workspaceId))
                .andExpect(jsonPath("$.data.name").value("CS189 - Machine Learning"));

        // 3. Other user should be forbidden from accessing this private workspace
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // 4. Update Workspace
        UpdateWorkspaceRequest updateReq = new UpdateWorkspaceRequest("CS189 - Advanced ML", "Updated description");
        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("CS189 - Advanced ML"));

        // 5. Delete Workspace
        mockMvc.perform(delete("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // 6. Verify Deleted
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated request to workspaces should return 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedWorkspaceList() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized());
    }
}

