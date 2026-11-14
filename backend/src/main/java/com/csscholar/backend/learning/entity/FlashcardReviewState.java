package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.ReviewRating;
import com.csscholar.backend.learning.enums.ReviewState;
import com.csscholar.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_review_state")
public class FlashcardReviewState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private Flashcard card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReviewState state = ReviewState.NEW;

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount = 0;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "last_rating", length = 20)
    private ReviewRating lastRating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FlashcardReviewState() {}

    public FlashcardReviewState(Flashcard card, User user) {
        this.card = card;
        this.user = user;
        this.state = ReviewState.NEW;
        this.repetitionCount = 0;
        this.easeFactor = 2.5;
        this.intervalDays = 0;
        this.nextDueAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Flashcard getCard() { return card; }
    public void setCard(Flashcard card) { this.card = card; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ReviewState getState() { return state; }
    public void setState(ReviewState state) { this.state = state; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }

    public ReviewRating getLastRating() { return lastRating; }
    public void setLastRating(ReviewRating lastRating) { this.lastRating = lastRating; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

