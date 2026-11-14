package com.csscholar.backend.knowledge.entity;

import com.csscholar.backend.library.entity.LibraryResource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_topic_resources", uniqueConstraints = {
    @UniqueConstraint(name = "uq_topic_resource", columnNames = {"topic_id", "resource_id"})
})
public class KnowledgeTopicResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private KnowledgeTopic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private LibraryResource resource;

    @Column(name = "relevance_notes", columnDefinition = "TEXT")
    private String relevanceNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public KnowledgeTopicResource() {}

    public KnowledgeTopicResource(KnowledgeTopic topic, LibraryResource resource, String relevanceNotes) {
        this.topic = topic;
        this.resource = resource;
        this.relevanceNotes = relevanceNotes;
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

    public LibraryResource getResource() {
        return resource;
    }

    public void setResource(LibraryResource resource) {
        this.resource = resource;
    }

    public String getRelevanceNotes() {
        return relevanceNotes;
    }

    public void setRelevanceNotes(String relevanceNotes) {
        this.relevanceNotes = relevanceNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

