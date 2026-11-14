package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.LearningDifficultyLevel;
import com.csscholar.backend.learning.enums.LearningSourceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizPracticeDto {

    private UUID id;
    private String title;
    private String description;
    private LearningSourceType sourceType;
    private String sourceId;
    private LearningDifficultyLevel difficultyLevel;
    private int totalQuestions;
    private Integer timeLimitMinutes;
    private List<QuizQuestionPracticeDto> questions = new ArrayList<>();
    private Instant createdAt;

    public QuizPracticeDto() {}

    public QuizPracticeDto(UUID id, String title, String description, LearningSourceType sourceType,
                           String sourceId, LearningDifficultyLevel difficultyLevel, int totalQuestions,
                           Integer timeLimitMinutes, List<QuizQuestionPracticeDto> questions, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.difficultyLevel = difficultyLevel;
        this.totalQuestions = totalQuestions;
        this.timeLimitMinutes = timeLimitMinutes;
        this.questions = questions;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LearningSourceType getSourceType() { return sourceType; }
    public void setSourceType(LearningSourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public LearningDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public List<QuizQuestionPracticeDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionPracticeDto> questions) { this.questions = questions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

