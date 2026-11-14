package com.csscholar.backend.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class SearchRequest {

    @NotBlank(message = "Search query cannot be blank.")
    @Size(max = 1000, message = "Search query cannot exceed 1000 characters.")
    private String query;

    @NotNull(message = "Workspace ID is required.")
    private UUID workspaceId;

    @Min(value = 1, message = "topK must be at least 1.")
    @Max(value = 100, message = "topK cannot exceed 100.")
    private Integer topK = 10;

    public SearchRequest() {}

    public SearchRequest(String query, UUID workspaceId, Integer topK) {
        this.query = query;
        this.workspaceId = workspaceId;
        this.topK = topK != null ? topK : 10;
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

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK != null ? topK : 10;
    }
}

