package com.csscholar.backend.chat.dto;

import java.time.Instant;
import java.util.UUID;

public class ConversationResponse {

    private UUID id;
    private UUID workspaceId;
    private String title;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastMessageAt;
    private int messageCount;

    public ConversationResponse() {}

    public ConversationResponse(UUID id, UUID workspaceId, String title, String status,
                                Instant createdAt, Instant updatedAt, Instant lastMessageAt,
                                int messageCount) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastMessageAt = lastMessageAt;
        this.messageCount = messageCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}

