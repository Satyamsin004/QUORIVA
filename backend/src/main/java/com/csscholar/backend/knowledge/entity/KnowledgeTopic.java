package com.csscholar.backend.knowledge.entity;

import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.TopicStatus;
import com.csscholar.backend.knowledge.enums.TopicType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "knowledge_topics")
public class KnowledgeTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private AcademicSubject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_topic_id")
    private KnowledgeTopic parentTopic;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 50)
    private DifficultyLevel difficultyLevel = DifficultyLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false, length = 50)
    private TopicType topicType = TopicType.CONCEPT;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TopicStatus status = TopicStatus.PUBLISHED;

    @Column(name = "importance_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal importanceScore = BigDecimal.valueOf(1.0);

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<KnowledgeTopicLearningObjective> learningObjectives = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<KnowledgeTopicTag> tags = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public KnowledgeTopic() {}

    public KnowledgeTopic(AcademicSubject subject, String name, String slug, String summary, String description,
                          DifficultyLevel difficultyLevel, TopicType topicType, int estimatedMinutes, BigDecimal importanceScore) {
        this.subject = subject;
        this.name = name;
        this.slug = slug;
        this.summary = summary;
        this.description = description;
        this.difficultyLevel = difficultyLevel;
        this.topicType = topicType;
        this.estimatedMinutes = estimatedMinutes;
        this.importanceScore = importanceScore != null ? importanceScore : BigDecimal.valueOf(1.0);
        this.status = TopicStatus.PUBLISHED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AcademicSubject getSubject() {
        return subject;
    }

    public void setSubject(AcademicSubject subject) {
        this.subject = subject;
    }

    public KnowledgeTopic getParentTopic() {
        return parentTopic;
    }

    public void setParentTopic(KnowledgeTopic parentTopic) {
        this.parentTopic = parentTopic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public void setTopicType(TopicType topicType) {
        this.topicType = topicType;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public TopicStatus getStatus() {
        return status;
    }

    public void setStatus(TopicStatus status) {
        this.status = status;
    }

    public BigDecimal getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(BigDecimal importanceScore) {
        this.importanceScore = importanceScore;
    }

    public List<KnowledgeTopicLearningObjective> getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(List<KnowledgeTopicLearningObjective> learningObjectives) {
        this.learningObjectives = learningObjectives;
    }

    public List<KnowledgeTopicTag> getTags() {
        return tags;
    }

    public void setTags(List<KnowledgeTopicTag> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

