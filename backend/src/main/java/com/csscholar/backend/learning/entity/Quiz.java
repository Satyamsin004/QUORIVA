package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.LearningDifficultyLevel;
import com.csscholar.backend.learning.enums.LearningSourceType;
import com.csscholar.backend.user.User;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private LearningSourceType sourceType = LearningSourceType.WORKSPACE_DOCUMENTS;

    @Column(name = "source_id", length = 255)
    private String sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private KnowledgeTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 50)
    private LearningDifficultyLevel difficultyLevel = LearningDifficultyLevel.MEDIUM;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionIndex ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Quiz() {}

    public Quiz(User user, Workspace workspace, String title, String description,
                LearningSourceType sourceType, String sourceId, KnowledgeTopic topic,
                LearningDifficultyLevel difficultyLevel, Integer timeLimitMinutes) {
        this.user = user;
        this.workspace = workspace;
        this.title = title;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.topic = topic;
        this.difficultyLevel = difficultyLevel;
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public void addQuestion(QuizQuestion question) {
        questions.add(question);
        question.setQuiz(this);
        this.totalQuestions = questions.size();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LearningSourceType getSourceType() { return sourceType; }
    public void setSourceType(LearningSourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public KnowledgeTopic getTopic() { return topic; }
    public void setTopic(KnowledgeTopic topic) { this.topic = topic; }

    public LearningDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
        this.totalQuestions = questions != null ? questions.size() : 0;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

