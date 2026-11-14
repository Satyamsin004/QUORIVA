package com.csscholar.backend.knowledge.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_topic_tags", uniqueConstraints = {
    @UniqueConstraint(name = "uq_knowledge_topic_tag", columnNames = {"topic_id", "tag"})
})
public class KnowledgeTopicTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private KnowledgeTopic topic;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public KnowledgeTopicTag() {}

    public KnowledgeTopicTag(KnowledgeTopic topic, String tag) {
        this.topic = topic;
        this.tag = tag;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

