package com.csscholar.backend.knowledge.entity;

import com.csscholar.backend.knowledge.enums.RelationshipType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_topic_relationships", uniqueConstraints = {
    @UniqueConstraint(name = "uq_topic_relationship", columnNames = {"source_topic_id", "target_topic_id", "relationship_type"})
})
public class KnowledgeTopicRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_topic_id", nullable = false)
    private KnowledgeTopic sourceTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_topic_id", nullable = false)
    private KnowledgeTopic targetTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_bidirectional", nullable = false)
    private boolean isBidirectional = false;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight = BigDecimal.valueOf(1.0);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public KnowledgeTopicRelationship() {}

    public KnowledgeTopicRelationship(KnowledgeTopic sourceTopic, KnowledgeTopic targetTopic, RelationshipType relationshipType, String description, BigDecimal weight) {
        this.sourceTopic = sourceTopic;
        this.targetTopic = targetTopic;
        this.relationshipType = relationshipType;
        this.description = description;
        this.weight = weight != null ? weight : BigDecimal.valueOf(1.0);
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public KnowledgeTopic getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(KnowledgeTopic sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public KnowledgeTopic getTargetTopic() {
        return targetTopic;
    }

    public void setTargetTopic(KnowledgeTopic targetTopic) {
        this.targetTopic = targetTopic;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBidirectional() {
        return isBidirectional;
    }

    public void setBidirectional(boolean bidirectional) {
        isBidirectional = bidirectional;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

