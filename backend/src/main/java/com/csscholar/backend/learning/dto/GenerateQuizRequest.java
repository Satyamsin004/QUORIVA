package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.LearningDifficultyLevel;
import com.csscholar.backend.learning.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateQuizRequest {

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

    @JsonProperty("difficultyLevel")
    @JsonAlias({"difficulty", "difficulty_level"})
    private LearningDifficultyLevel difficultyLevel = LearningDifficultyLevel.MEDIUM;

    @Min(1)
    @Max(20)
    private int questionCount = 5;

    private List<QuestionType> questionTypes = List.of(QuestionType.MULTIPLE_CHOICE);
    private String conceptFocus;
    private Integer timeLimitMinutes;

    public GenerateQuizRequest() {}

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

    public LearningDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public List<QuestionType> getQuestionTypes() { return questionTypes; }
    public void setQuestionTypes(List<QuestionType> questionTypes) { this.questionTypes = questionTypes; }

    public String getConceptFocus() { return conceptFocus; }
    public void setConceptFocus(String conceptFocus) { this.conceptFocus = conceptFocus; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
}

