package com.csscholar.backend.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateConversationRequest {

    @NotNull(message = "workspaceId is required")
    private UUID workspaceId;

    private String title;

    public CreateConversationRequest() {}

    public CreateConversationRequest(UUID workspaceId, String title) {
        this.workspaceId = workspaceId;
        this.title = title;
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
}

