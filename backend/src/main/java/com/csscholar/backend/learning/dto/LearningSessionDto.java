package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.SessionType;

import java.time.Instant;
import java.util.UUID;

public class LearningSessionDto {

    private UUID id;
    private SessionType sessionType;
    private UUID referenceId;
    private int durationSeconds;
    private int itemsCompleted;
    private Double scorePercentage;
    private Instant createdAt;

    public LearningSessionDto() {}

    public LearningSessionDto(UUID id, SessionType sessionType, UUID referenceId,
                              int durationSeconds, int itemsCompleted, Double scorePercentage, Instant createdAt) {
        this.id = id;
        this.sessionType = sessionType;
        this.referenceId = referenceId;
        this.durationSeconds = durationSeconds;
        this.itemsCompleted = itemsCompleted;
        this.scorePercentage = scorePercentage;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SessionType getSessionType() { return sessionType; }
    public void setSessionType(SessionType sessionType) { this.sessionType = sessionType; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getItemsCompleted() { return itemsCompleted; }
    public void setItemsCompleted(int itemsCompleted) { this.itemsCompleted = itemsCompleted; }

    public Double getScorePercentage() { return scorePercentage; }
    public void setScorePercentage(Double scorePercentage) { this.scorePercentage = scorePercentage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

