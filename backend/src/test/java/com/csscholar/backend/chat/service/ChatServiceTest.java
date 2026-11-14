package com.csscholar.backend.chat.service;

import com.csscholar.backend.chat.client.TutorClient;
import com.csscholar.backend.chat.dto.MessageResponse;
import com.csscholar.backend.chat.dto.SendMessageRequest;
import com.csscholar.backend.chat.dto.TutorChatInternalRequest;
import com.csscholar.backend.chat.dto.TutorChatInternalResponse;
import com.csscholar.backend.chat.dto.TutorCitationDto;
import com.csscholar.backend.chat.entity.Conversation;
import com.csscholar.backend.chat.entity.Message;
import com.csscholar.backend.chat.repository.ConversationRepository;
import com.csscholar.backend.chat.repository.MessageRepository;
import com.csscholar.backend.chat.repository.MessageSourceRepository;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.DocumentChunk;
import com.csscholar.backend.document.repository.DocumentChunkRepository;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceResponse;
import com.csscholar.backend.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageSourceRepository messageSourceRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private TutorClient tutorClient;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                messageSourceRepository,
                documentRepository,
                documentChunkRepository,
                workspaceService,
                tutorClient
        );
    }

    @Test
    @DisplayName("sendMessage should persist user message, invoke tutor client, and persist assistant message with sources")
    void sendMessage_Success() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        User user = new User("Alice", "alice@example.com", "hash", Role.STUDENT);
        user.setId(userId);
        Workspace workspace = new Workspace("OS", "Notes", user);
        workspace.setId(workspaceId);

        Conversation conv = new Conversation(user, workspace, "New Conversation");
        conv.setId(conversationId);
        conv.setCreatedAt(Instant.now());
        conv.setUpdatedAt(Instant.now());
        conv.setLastMessageAt(Instant.now());

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conv));
        when(workspaceService.getWorkspaceById(userId, workspaceId)).thenReturn(mock(WorkspaceResponse.class));

        // Tutor response
        TutorChatInternalResponse tutorResponse = new TutorChatInternalResponse();
        tutorResponse.setAnswer("Paging avoids external fragmentation [S1].");
        tutorResponse.setGroundingStatus("GROUNDED");
        tutorResponse.setInsufficientEvidence(false);
        tutorResponse.setProvider("groq");
        tutorResponse.setModelName("llama-3.3-70b-versatile");
        tutorResponse.setInputTokens(100);
        tutorResponse.setOutputTokens(25);
        tutorResponse.setLatencyMs(200L);

        TutorCitationDto cit = new TutorCitationDto();
        cit.setSourceId("S1");
        cit.setDocumentId(docId);
        cit.setChunkId(chunkId);
        cit.setRank(1);
        cit.setRelevanceScore(0.95);
        tutorResponse.setCitations(Collections.singletonList(cit));

        when(tutorClient.generateAnswer(any(TutorChatInternalRequest.class))).thenReturn(tutorResponse);

        Document mockDoc = mock(Document.class);
        when(mockDoc.getId()).thenReturn(docId);
        when(mockDoc.getTitle()).thenReturn("Silberschatz.pdf");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(mockDoc));

        DocumentChunk mockChunk = mock(DocumentChunk.class);
        when(mockChunk.getId()).thenReturn(chunkId);
        when(mockChunk.getPageNumber()).thenReturn(150);
        when(documentChunkRepository.findById(chunkId)).thenReturn(Optional.of(mockChunk));

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId(UUID.randomUUID());
            }
            if (m.getCreatedAt() == null) {
                m.setCreatedAt(Instant.now());
            }
            return m;
        });

        SendMessageRequest req = new SendMessageRequest("What is paging?", "groq", "llama-3.3-70b-versatile");
        MessageResponse response = chatService.sendMessage(userId, conversationId, req);

        assertNotNull(response);
        assertEquals("ASSISTANT", response.getRole());
        assertEquals("GROUNDED", response.getStatus());
        assertEquals("groq", response.getProvider());
        assertEquals(200L, response.getLatencyMs());
        assertEquals(1, response.getSources().size());
        assertEquals("S1", response.getSources().get(0).getCitationLabel());
        assertEquals("Silberschatz.pdf", response.getSources().get(0).getDocumentTitle());

        // Conversation title should be auto-updated from "New Conversation"
        verify(conversationRepository).save(conv);
        assertEquals("What is paging?", conv.getTitle());
    }

    @Test
    @DisplayName("getMessages should return list of messages with sources")
    void getMessages_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        User user = new User("Bob", "bob@example.com", "hash", Role.STUDENT);
        user.setId(userId);
        Workspace workspace = new Workspace("Workspace", "", user);
        workspace.setId(workspaceId);

        Conversation conv = new Conversation(user, workspace, "Discussion");
        conv.setId(conversationId);

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conv));

        Message msg = new Message(conv, "USER", "Hello");
        msg.setId(UUID.randomUUID());
        msg.setCreatedAt(Instant.now());
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.singletonList(msg));

        var messages = chatService.getMessages(userId, conversationId);
        assertEquals(1, messages.size());
        assertEquals("USER", messages.get(0).getRole());
        assertEquals("Hello", messages.get(0).getContent());
    }
}

