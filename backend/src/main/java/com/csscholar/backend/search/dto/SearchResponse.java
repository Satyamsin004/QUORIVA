package com.csscholar.backend.search.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SearchResponse {

    private String query;
    private UUID workspaceId;
    private int totalCandidates;
    private List<SearchResultDto> results = new ArrayList<>();

    public SearchResponse() {}

    public SearchResponse(String query, UUID workspaceId, int totalCandidates, List<SearchResultDto> results) {
        this.query = query;
        this.workspaceId = workspaceId;
        this.totalCandidates = totalCandidates;
        this.results = results != null ? results : new ArrayList<>();
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

    public int getTotalCandidates() {
        return totalCandidates;
    }

    public void setTotalCandidates(int totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    public List<SearchResultDto> getResults() {
        return results;
    }

    public void setResults(List<SearchResultDto> results) {
        this.results = results;
    }
}

