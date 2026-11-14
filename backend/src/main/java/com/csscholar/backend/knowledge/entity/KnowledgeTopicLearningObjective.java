package com.csscholar.backend.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_topic_learning_objectives")
public class KnowledgeTopicLearningObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private KnowledgeTopic topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objective;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public KnowledgeTopicLearningObjective() {}

    public KnowledgeTopicLearningObjective(KnowledgeTopic topic, String objective, int displayOrder) {
        this.topic = topic;
        this.objective = objective;
        this.displayOrder = displayOrder;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public KnowledgeTopic getTopic() {
        return topic;
    }

    public void setTopic(KnowledgeTopic topic) {
        this.topic = topic;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

