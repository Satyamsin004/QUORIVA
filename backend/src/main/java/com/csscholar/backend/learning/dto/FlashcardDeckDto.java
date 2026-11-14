package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.LearningSourceType;

import java.time.Instant;
import java.util.UUID;

public class FlashcardDeckDto {

    private UUID id;
    private String title;
    private String description;
    private String subjectName;
    private String topicName;
    private LearningSourceType sourceType;
    private int totalCards;
    private int dueCardsCount;
    private Instant createdAt;
    private Instant updatedAt;

    public FlashcardDeckDto() {}

    public FlashcardDeckDto(UUID id, String title, String description, String subjectName,
                            String topicName, LearningSourceType sourceType, int totalCards,
                            int dueCardsCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.subjectName = subjectName;
        this.topicName = topicName;
        this.sourceType = sourceType;
        this.totalCards = totalCards;
        this.dueCardsCount = dueCardsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public LearningSourceType getSourceType() { return sourceType; }
    public void setSourceType(LearningSourceType sourceType) { this.sourceType = sourceType; }

    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public int getDueCardsCount() { return dueCardsCount; }
    public void setDueCardsCount(int dueCardsCount) { this.dueCardsCount = dueCardsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

