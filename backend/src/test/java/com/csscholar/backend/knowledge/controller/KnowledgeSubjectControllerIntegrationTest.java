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
class KnowledgeSubjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "know_subj_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest("Knowledge User", email, "Password123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Lists all academic subjects in display order with topic counts")
    void testGetAllSubjects() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/subjects")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(8)))
                .andExpect(jsonPath("$.data[0].slug").value("programming-fundamentals"))
                .andExpect(jsonPath("$.data[0].code").value("CS-PROG"))
                .andExpect(jsonPath("$.data[0].topicCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Retrieves subject by slug and subject topics")
    void testGetSubjectBySlugAndTopics() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/knowledge/subjects/slug/data-structures-and-algorithms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Data Structures and Algorithms"))
                .andExpect(jsonPath("$.data.code").value("CS-DSA"))
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        String subjectId = root.path("data").path("id").asText();

        // Query topics under subject
        mockMvc.perform(get("/api/v1/knowledge/subjects/" + subjectId + "/topics")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)));
    }
}

