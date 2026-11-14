package com.csscholar.backend.search.controller;

import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.search.client.SearchClient;
import com.csscholar.backend.search.dto.SearchRequest;
import com.csscholar.backend.search.dto.SearchResponse;
import com.csscholar.backend.search.dto.SearchResultDto;
import com.csscholar.backend.workspace.CreateWorkspaceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchClient searchClient;

    private String userToken;
    private String otherUserToken;
    private UUID userWorkspaceId;

    @BeforeEach
    void setUp() throws Exception {
        // Register primary user
        String email1 = "search_alice_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req1 = new RegisterRequest("Alice Researcher", email1, "Password123!");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        userToken = root1.get("data").get("accessToken").asText();

        // Create workspace for user 1
        CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest("Algorithms 101", "Lecture Notes");
        MvcResult wsRes = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode wsRoot = objectMapper.readTree(wsRes.getResponse().getContentAsString());
        userWorkspaceId = UUID.fromString(wsRoot.get("data").get("id").asText());

        // Register second user
        String email2 = "search_bob_" + UUID.randomUUID() + "@example.com";
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
    @DisplayName("search should return 200 OK with ranked results for authorized user")
    void shouldReturn200ForAuthorizedSearch() throws Exception {
        SearchResultDto item = new SearchResultDto();
        item.setChunkId(UUID.randomUUID());
        item.setDocumentId(UUID.randomUUID());
        item.setDocumentName("BinarySearchTrees.pdf");
        item.setWorkspaceId(userWorkspaceId);
        item.setChunkContent("A binary search tree maintains sorted order.");
        item.setPageNumber(15);
        item.setSectionHeading("BST Operations");
        item.setChunkIndex(2);
        item.setSourceType("PDF");
        item.setFinalRank(1);
        item.setRerankerScore(0.94);
        item.setRrfScore(0.032);
        item.setDenseRank(1);
        item.setKeywordRank(1);
        item.setDenseSimilarity(0.89);
        item.setKeywordScore(0.48);

        SearchResponse mockResponse = new SearchResponse("binary search tree", userWorkspaceId, 1, Collections.singletonList(item));
        when(searchClient.executeSearch(eq("binary search tree"), eq(userWorkspaceId), any(), eq(10)))
                .thenReturn(mockResponse);

        SearchRequest request = new SearchRequest("binary search tree", userWorkspaceId, 10);

        mockMvc.perform(post("/api/v1/search")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value("binary search tree"))
                .andExpect(jsonPath("$.data.workspaceId").value(userWorkspaceId.toString()))
                .andExpect(jsonPath("$.data.totalCandidates").value(1))
                .andExpect(jsonPath("$.data.results[0].documentName").value("BinarySearchTrees.pdf"))
                .andExpect(jsonPath("$.data.results[0].finalRank").value(1))
                .andExpect(jsonPath("$.data.results[0].pageNumber").value(15));
    }

    @Test
    @DisplayName("search should return 401 Unauthorized when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        SearchRequest request = new SearchRequest("binary search tree", userWorkspaceId, 10);

        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("search should return 403 Forbidden when user attempts to search another user's workspace")
    void shouldReturn403WhenSearchingOtherUserWorkspace() throws Exception {
        SearchRequest request = new SearchRequest("binary search tree", userWorkspaceId, 10);

        mockMvc.perform(post("/api/v1/search")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("search should return 400 Bad Request when query is blank")
    void shouldReturn400WhenQueryIsBlank() throws Exception {
        SearchRequest request = new SearchRequest("   ", userWorkspaceId, 10);

        mockMvc.perform(post("/api/v1/search")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("search should return 400 Bad Request when topK exceeds maximum")
    void shouldReturn400WhenTopKExceedsMax() throws Exception {
        SearchRequest request = new SearchRequest("query", userWorkspaceId, 101);

        mockMvc.perform(post("/api/v1/search")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

