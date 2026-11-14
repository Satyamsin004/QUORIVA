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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeTopicControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "know_topic_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest("Topic User", email, "Password123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Lists topics with search query, difficulty filter, and pagination")
    void testSearchAndFilterTopics() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/topics?q=recursion")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].slug").value("recursion"));

        // Filter by difficulty ADVANCED
        mockMvc.perform(get("/api/v1/knowledge/topics?difficulty=ADVANCED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("Retrieves detailed topic including learning objectives, prerequisites, and unlocked concepts")
    void testGetTopicDetail() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/topics/dynamic-programming")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Dynamic Programming"))
                .andExpect(jsonPath("$.data.slug").value("dynamic-programming"))
                .andExpect(jsonPath("$.data.difficultyLevel").value("ADVANCED"))
                .andExpect(jsonPath("$.data.topicType").value("ALGORITHM"))
                .andExpect(jsonPath("$.data.learningObjectives").isArray())
                .andExpect(jsonPath("$.data.learningObjectives.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.prerequisites").isArray())
                .andExpect(jsonPath("$.data.prerequisites.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Retrieves direct prerequisites for a given topic")
    void testGetPrerequisites() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/topics/dynamic-programming/prerequisites")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Retrieves related topics for a given topic")
    void testGetRelatedTopics() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/topics/indexing-query-optimization/related")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}

