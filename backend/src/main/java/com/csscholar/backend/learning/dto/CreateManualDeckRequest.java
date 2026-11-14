package com.csscholar.backend.learning.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CreateManualDeckRequest {

    private UUID workspaceId;

    @NotBlank(message = "Deck title is required")
    private String title;

    private String description;
    private String subjectName;
    private String topicName;

    public CreateManualDeckRequest() {}

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
}

