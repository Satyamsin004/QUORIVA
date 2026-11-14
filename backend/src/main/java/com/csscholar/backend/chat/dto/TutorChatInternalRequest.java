package com.csscholar.backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TutorChatInternalRequest {

    @JsonProperty("query")
    private String query;

    @JsonProperty("workspace_id")
    @JsonAlias({"workspaceId", "workspace_id"})
    private UUID workspaceId;

    @JsonProperty("user_id")
    @JsonAlias({"userId", "user_id"})
    private UUID userId;

    @JsonProperty("conversation_id")
    @JsonAlias({"conversationId", "conversation_id"})
    private UUID conversationId;

    @JsonProperty("history")
    @JsonAlias({"history", "conversation_history"})
    private List<Map<String, String>> history = new ArrayList<>();

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("model_name")
    @JsonAlias({"modelName", "model_name"})
    private String modelName;

    public TutorChatInternalRequest() {}

    public TutorChatInternalRequest(String query, UUID workspaceId, UUID userId, UUID conversationId,
                                    List<Map<String, String>> history, String provider, String modelName) {
        this.query = query;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.history = history != null ? history : new ArrayList<>();
        this.provider = provider;
        this.modelName = modelName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}

