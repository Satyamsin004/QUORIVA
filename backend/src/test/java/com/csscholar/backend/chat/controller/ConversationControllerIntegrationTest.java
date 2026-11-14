package com.csscholar.backend.chat.controller;

import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.chat.client.TutorClient;
import com.csscholar.backend.chat.dto.*;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TutorClient tutorClient;

    private String userToken;
    private String otherUserToken;
    private UUID userWorkspaceId;

    @BeforeEach
    void setUp() throws Exception {
        // Register User A
        String email1 = "chat_alice_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req1 = new RegisterRequest("Alice Chat", email1, "Password123!");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        userToken = root1.path("data").path("accessToken").asText();

        // Create Workspace for User A
        CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest("Alice Operating Systems", "OS materials");
        MvcResult wsRes = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode wsRoot = objectMapper.readTree(wsRes.getResponse().getContentAsString());
        userWorkspaceId = UUID.fromString(wsRoot.path("data").path("id").asText());

        // Register User B
        String email2 = "chat_bob_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req2 = new RegisterRequest("Bob Chat", email2, "Password123!");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        otherUserToken = root2.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("POST /api/v1/conversations should create conversation for authorized workspace")
    void createConversation_Success() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest(userWorkspaceId, "Virtual Memory Discussion");

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Virtual Memory Discussion"))
                .andExpect(jsonPath("$.data.workspaceId").value(userWorkspaceId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/conversations should reject unauthorized workspace access")
    void createConversation_UnauthorizedWorkspace() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest(userWorkspaceId, "Bob Intrusion");

        // User B tries to create conversation in User A's workspace
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Full Conversation and Chat Message Lifecycle")
    void conversationAndMessageLifecycle_Success() throws Exception {
        // 1. Create conversation
        CreateConversationRequest createReq = new CreateConversationRequest(userWorkspaceId, "Concurrency & Deadlocks");
        MvcResult convRes = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(convRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. List conversations
        mockMvc.perform(get("/api/v1/conversations?workspaceId=" + userWorkspaceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(convId))
                .andExpect(jsonPath("$.data.content[0].title").value("Concurrency & Deadlocks"));

        // 3. Mock Tutor Client response
        TutorChatInternalResponse tutorResponse = new TutorChatInternalResponse();
        tutorResponse.setAnswer("Deadlock occurs when four Coffman conditions hold [S1].");
        tutorResponse.setGroundingStatus("GROUNDED");
        tutorResponse.setInsufficientEvidence(false);
        tutorResponse.setProvider("groq");
        tutorResponse.setModelName("llama-3.3-70b-versatile");
        tutorResponse.setInputTokens(120);
        tutorResponse.setOutputTokens(35);
        tutorResponse.setLatencyMs(190L);

        TutorCitationDto citation = new TutorCitationDto();
        citation.setSourceId("S1");
        citation.setRank(1);
        citation.setRelevanceScore(0.92);
        tutorResponse.setCitations(Collections.singletonList(citation));

        when(tutorClient.generateAnswer(any(TutorChatInternalRequest.class))).thenReturn(tutorResponse);

        // 4. Send Message
        SendMessageRequest sendReq = new SendMessageRequest("What causes deadlocks?", "groq", "llama-3.3-70b-versatile");
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.status").value("GROUNDED"))
                .andExpect(jsonPath("$.data.provider").value("groq"))
                .andExpect(jsonPath("$.data.content").value("Deadlock occurs when four Coffman conditions hold [S1]."))
                .andExpect(jsonPath("$.data.sources[0].citationLabel").value("S1"));

        // 5. Get Messages
        mockMvc.perform(get("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].content").value("What causes deadlocks?"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));

        // 6. Delete Conversation
        mockMvc.perform(delete("/api/v1/conversations/" + convId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Verify conversation is gone
        mockMvc.perform(get("/api/v1/conversations/" + convId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}
