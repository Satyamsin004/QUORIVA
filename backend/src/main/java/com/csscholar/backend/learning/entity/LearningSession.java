package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.SessionType;
import com.csscholar.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_sessions")
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 50)
    private SessionType sessionType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds = 0;

    @Column(name = "items_completed", nullable = false)
    private int itemsCompleted = 0;

    @Column(name = "score_percentage")
    private Double scorePercentage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LearningSession() {}

    public LearningSession(User user, SessionType sessionType, UUID referenceId,
                           int durationSeconds, int itemsCompleted, Double scorePercentage) {
        this.user = user;
        this.sessionType = sessionType;
        this.referenceId = referenceId;
        this.durationSeconds = durationSeconds;
        this.itemsCompleted = itemsCompleted;
        this.scorePercentage = scorePercentage;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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
}

