package com.csscholar.backend.chat.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.chat.dto.*;
import com.csscholar.backend.chat.service.ChatService;
import com.csscholar.backend.chat.service.ConversationService;
import com.csscholar.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    public ConversationController(ConversationService conversationService, ChatService chatService) {
        this.conversationService = conversationService;
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        ConversationResponse response = conversationService.createConversation(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> listConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationResponse> response = conversationService.listConversations(principal.getId(), workspaceId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        ConversationResponse response = conversationService.getConversation(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        conversationService.deleteConversation(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully.", null));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        List<MessageResponse> response = chatService.getMessages(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully.", response));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = chatService.sendMessage(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent and answer generated successfully.", response));
    }
}

