package com.csscholar.backend.learning.entity;

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
@Table(name = "flashcard_decks")
public class FlashcardDeck {

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

    @Column(name = "subject_name", length = 150)
    private String subjectName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private KnowledgeTopic topic;

    @Column(name = "topic_name", length = 255)
    private String topicName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private LearningSourceType sourceType = LearningSourceType.MANUAL;

    @Column(name = "source_id", length = 255)
    private String sourceId;

    @Column(name = "total_cards", nullable = false)
    private int totalCards = 0;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Flashcard> cards = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FlashcardDeck() {}

    public FlashcardDeck(User user, Workspace workspace, String title, String description,
                         String subjectName, KnowledgeTopic topic, String topicName,
                         LearningSourceType sourceType, String sourceId) {
        this.user = user;
        this.workspace = workspace;
        this.title = title;
        this.description = description;
        this.subjectName = subjectName;
        this.topic = topic;
        this.topicName = topicName;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    public void addCard(Flashcard card) {
        cards.add(card);
        card.setDeck(this);
        this.totalCards = cards.size();
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

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public KnowledgeTopic getTopic() { return topic; }
    public void setTopic(KnowledgeTopic topic) { this.topic = topic; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public LearningSourceType getSourceType() { return sourceType; }
    public void setSourceType(LearningSourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public List<Flashcard> getCards() { return cards; }
    public void setCards(List<Flashcard> cards) {
        this.cards = cards;
        this.totalCards = cards != null ? cards.size() : 0;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

