package com.csscholar.backend.knowledge.controller;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgePathControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "know_path_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest("Path User", email, "Password123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Generates deterministic learning path for dynamic programming with ordered prerequisites")
    void testGetLearningPathForDynamicProgramming() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/topics/dynamic-programming/learning-path")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetTopicSlug").value("dynamic-programming"))
                .andExpect(jsonPath("$.data.hasPrerequisites").value(true))
                .andExpect(jsonPath("$.data.totalSteps").value(greaterThan(1)))
                .andExpect(jsonPath("$.data.estimatedTotalMinutes").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.steps").isArray())
                // The last step must be the target topic
                .andExpect(jsonPath("$.data.steps[-1].slug").value("dynamic-programming"));
    }

    @Test
    @DisplayName("Retrieves curated learning paths for core milestones")
    void testGetCuratedLearningPaths() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/learning-paths")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(3)));
    }
}

