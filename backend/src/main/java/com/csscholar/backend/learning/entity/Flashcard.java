package com.csscholar.backend.learning.entity;

import com.csscholar.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private FlashcardDeck deck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String front;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String back;

    @Column(name = "topic_name", length = 255)
    private String topicName;

    @Column(name = "source_reference", length = 512)
    private String sourceReference;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "slide_number")
    private Integer slideNumber;

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private FlashcardReviewState reviewState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Flashcard() {}

    public Flashcard(FlashcardDeck deck, User user, String front, String back,
                     String topicName, String sourceReference, Integer pageNumber, Integer slideNumber) {
        this.deck = deck;
        this.user = user;
        this.front = front;
        this.back = back;
        this.topicName = topicName;
        this.sourceReference = sourceReference;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public FlashcardDeck getDeck() { return deck; }
    public void setDeck(FlashcardDeck deck) { this.deck = deck; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }

    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getSlideNumber() { return slideNumber; }
    public void setSlideNumber(Integer slideNumber) { this.slideNumber = slideNumber; }

    public FlashcardReviewState getReviewState() { return reviewState; }
    public void setReviewState(FlashcardReviewState reviewState) { this.reviewState = reviewState; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

