package com.csscholar.backend.chat.service;

import com.csscholar.backend.chat.dto.ConversationResponse;
import com.csscholar.backend.chat.dto.CreateConversationRequest;
import com.csscholar.backend.chat.entity.Conversation;
import com.csscholar.backend.chat.repository.ConversationRepository;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceRepository;
import com.csscholar.backend.workspace.WorkspaceResponse;
import com.csscholar.backend.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                workspaceService,
                workspaceRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("createConversation should verify workspace and persist new conversation")
    void createConversation_Success() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        User user = new User("Alice", "alice@example.com", "hash", Role.STUDENT);
        user.setId(userId);

        Workspace workspace = new Workspace("CS Study", "Notes", user);
        workspace.setId(workspaceId);

        WorkspaceResponse mockWorkspaceResponse = new WorkspaceResponse(
                workspaceId, "CS Study", "Notes", userId, "Alice", Instant.now(), Instant.now()
        );
        when(workspaceService.getWorkspaceById(userId, workspaceId)).thenReturn(mockWorkspaceResponse);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            c.setLastMessageAt(Instant.now());
            return c;
        });

        CreateConversationRequest req = new CreateConversationRequest(workspaceId, "Operating Systems Discussion");
        ConversationResponse response = conversationService.createConversation(userId, req);

        assertNotNull(response);
        assertEquals(workspaceId, response.getWorkspaceId());
        assertEquals("Operating Systems Discussion", response.getTitle());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("createConversation should throw ForbiddenException when workspace access is denied")
    void createConversation_ForbiddenWorkspace() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        when(workspaceService.getWorkspaceById(userId, workspaceId))
                .thenThrow(new ForbiddenException("You do not have permission to access this workspace."));

        CreateConversationRequest req = new CreateConversationRequest(workspaceId, "Forbidden Chat");

        assertThrows(ForbiddenException.class, () -> conversationService.createConversation(userId, req));
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("listConversations should return page of conversations for authorized workspace")
    void listConversations_Success() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User("Bob", "bob@example.com", "hash", Role.STUDENT);
        user.setId(userId);
        Workspace workspace = new Workspace("Distributed Systems", "Notes", user);
        workspace.setId(workspaceId);

        Conversation conv = new Conversation(user, workspace, "Raft Protocol");
        conv.setId(UUID.randomUUID());
        conv.setCreatedAt(Instant.now());
        conv.setUpdatedAt(Instant.now());
        conv.setLastMessageAt(Instant.now());

        Page<Conversation> page = new PageImpl<>(Collections.singletonList(conv), pageable, 1);

        when(workspaceService.getWorkspaceById(userId, workspaceId)).thenReturn(mock(WorkspaceResponse.class));
        when(conversationRepository.findByUserIdAndWorkspaceIdOrderByLastMessageAtDesc(userId, workspaceId, pageable))
                .thenReturn(page);

        Page<ConversationResponse> results = conversationService.listConversations(userId, workspaceId, pageable);

        assertEquals(1, results.getTotalElements());
        assertEquals("Raft Protocol", results.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("getConversation should return conversation if owned by user")
    void getConversation_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        User user = new User("Alice", "alice@example.com", "hash", Role.STUDENT);
        user.setId(userId);
        Workspace workspace = new Workspace("Workspace", "", user);
        workspace.setId(UUID.randomUUID());

        Conversation conv = new Conversation(user, workspace, "Algorithms");
        conv.setId(conversationId);
        conv.setCreatedAt(Instant.now());
        conv.setUpdatedAt(Instant.now());
        conv.setLastMessageAt(Instant.now());

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conv));

        ConversationResponse resp = conversationService.getConversation(userId, conversationId);

        assertNotNull(resp);
        assertEquals(conversationId, resp.getId());
        assertEquals("Algorithms", resp.getTitle());
    }

    @Test
    @DisplayName("deleteConversation should remove conversation when owned by user")
    void deleteConversation_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Conversation conv = mock(Conversation.class);
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conv));

        conversationService.deleteConversation(userId, conversationId);

        verify(conversationRepository).delete(conv);
    }
}

