package com.csscholar.backend.chat.service;

import com.csscholar.backend.chat.dto.ConversationResponse;
import com.csscholar.backend.chat.dto.CreateConversationRequest;
import com.csscholar.backend.chat.entity.Conversation;
import com.csscholar.backend.chat.repository.ConversationRepository;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceRepository;
import com.csscholar.backend.workspace.WorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            WorkspaceService workspaceService,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.workspaceService = workspaceService;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse createConversation(UUID userId, CreateConversationRequest request) {
        UUID workspaceId = request.getWorkspaceId();
        // Verify workspace access
        workspaceService.getWorkspaceById(userId, workspaceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : "New Conversation";

        Conversation conversation = new Conversation(user, workspace, title);
        Conversation saved = conversationRepository.save(conversation);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(UUID userId, UUID workspaceId, Pageable pageable) {
        // Verify workspace access
        workspaceService.getWorkspaceById(userId, workspaceId);

        return conversationRepository.findByUserIdAndWorkspaceIdOrderByLastMessageAtDesc(userId, workspaceId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or unauthorized: " + conversationId));
        return mapToResponse(conversation);
    }

    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found or unauthorized: " + conversationId));
        conversationRepository.delete(conversation);
    }

    public ConversationResponse mapToResponse(Conversation conversation) {
        int messageCount = conversation.getMessages() != null ? conversation.getMessages().size() : 0;
        return new ConversationResponse(
                conversation.getId(),
                conversation.getWorkspace().getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getLastMessageAt(),
                messageCount
        );
    }
}

