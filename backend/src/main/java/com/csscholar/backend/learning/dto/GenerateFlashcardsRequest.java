package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateFlashcardsRequest {

    @JsonProperty("workspaceId")
    @JsonAlias({"sourceWorkspaceId", "source_workspace_id", "workspace_id"})
    private UUID workspaceId;

    @JsonProperty("documentIds")
    @JsonAlias({"document_ids", "documentId", "document_id"})
    private List<UUID> documentIds;

    @JsonProperty("topicId")
    @JsonAlias({"topic_id"})
    private UUID topicId;

    @JsonProperty("topicSlug")
    @JsonAlias({"topic_slug"})
    private String topicSlug;

    @JsonProperty("libraryResourceId")
    @JsonAlias({"library_resource_id"})
    private UUID libraryResourceId;

    @JsonProperty("cardCount")
    @JsonAlias({"card_count", "count"})
    @Min(1)
    @Max(25)
    private int cardCount = 5;

    @JsonProperty("conceptFocus")
    @JsonAlias({"concept_focus", "focus"})
    private String conceptFocus;

    public GenerateFlashcardsRequest() {}

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public List<UUID> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<UUID> documentIds) { this.documentIds = documentIds; }

    public UUID getTopicId() { return topicId; }
    public void setTopicId(UUID topicId) { this.topicId = topicId; }

    public String getTopicSlug() { return topicSlug; }
    public void setTopicSlug(String topicSlug) { this.topicSlug = topicSlug; }

    public UUID getLibraryResourceId() { return libraryResourceId; }
    public void setLibraryResourceId(UUID libraryResourceId) { this.libraryResourceId = libraryResourceId; }

    public int getCardCount() { return cardCount; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }

    public String getConceptFocus() { return conceptFocus; }
    public void setConceptFocus(String conceptFocus) { this.conceptFocus = conceptFocus; }
}

