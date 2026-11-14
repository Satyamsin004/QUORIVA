package com.csscholar.backend.learning.dto.internal;

import java.util.List;
import java.util.UUID;

public class GenerateQuizInternalRequest {

    private UUID userId;
    private String sourceType;
    private String sourceTitle;
    private String contextText;
    private String difficultyLevel;
    private int questionCount;
    private List<String> questionTypes;
    private String conceptFocus;

    public GenerateQuizInternalRequest() {}

    public GenerateQuizInternalRequest(UUID userId, String sourceType, String sourceTitle,
                                       String contextText, String difficultyLevel, int questionCount,
                                       List<String> questionTypes, String conceptFocus) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceTitle = sourceTitle;
        this.contextText = contextText;
        this.difficultyLevel = difficultyLevel;
        this.questionCount = questionCount;
        this.questionTypes = questionTypes;
        this.conceptFocus = conceptFocus;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }

    public String getContextText() { return contextText; }
    public void setContextText(String contextText) { this.contextText = contextText; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public List<String> getQuestionTypes() { return questionTypes; }
    public void setQuestionTypes(List<String> questionTypes) { this.questionTypes = questionTypes; }

    public String getConceptFocus() { return conceptFocus; }
    public void setConceptFocus(String conceptFocus) { this.conceptFocus = conceptFocus; }
}

