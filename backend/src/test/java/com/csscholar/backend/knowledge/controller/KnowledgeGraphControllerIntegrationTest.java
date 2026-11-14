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
class KnowledgeGraphControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "know_graph_" + UUID.randomUUID() + "@example.com";
        RegisterRequest regReq = new RegisterRequest("Graph User", email, "Password123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Retrieves full knowledge graph with bounded nodes and edges")
    void testGetFullGraph() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/graph")
                        .param("maxNodes", "30")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.nodes.length()").value(lessThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.edges").isArray())
                .andExpect(jsonPath("$.data.totalNodes").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.totalEdges").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Retrieves topic neighborhood graph centered on a target topic")
    void testGetTopicNeighborhoodGraph() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/graph/topics/binary-search-trees")
                        .param("depth", "1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray())
                .andExpect(jsonPath("$.data.nodes[?(@.slug == 'binary-search-trees')].isCenter").value(true));
    }
}
