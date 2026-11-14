package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.ReviewRating;
import com.csscholar.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_review_history")
public class FlashcardReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Flashcard card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private FlashcardDeck deck;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewRating rating;

    @Column(name = "review_time_seconds")
    private Integer reviewTimeSeconds = 0;

    @Column(name = "scheduled_days", nullable = false)
    private int scheduledDays = 0;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor;

    @CreationTimestamp
    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    public FlashcardReviewHistory() {}

    public FlashcardReviewHistory(Flashcard card, FlashcardDeck deck, User user,
                                  ReviewRating rating, Integer reviewTimeSeconds,
                                  int scheduledDays, double easeFactor) {
        this.card = card;
        this.deck = deck;
        this.user = user;
        this.rating = rating;
        this.reviewTimeSeconds = reviewTimeSeconds;
        this.scheduledDays = scheduledDays;
        this.easeFactor = easeFactor;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Flashcard getCard() { return card; }
    public void setCard(Flashcard card) { this.card = card; }

    public FlashcardDeck getDeck() { return deck; }
    public void setDeck(FlashcardDeck deck) { this.deck = deck; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ReviewRating getRating() { return rating; }
    public void setRating(ReviewRating rating) { this.rating = rating; }

    public Integer getReviewTimeSeconds() { return reviewTimeSeconds; }
    public void setReviewTimeSeconds(Integer reviewTimeSeconds) { this.reviewTimeSeconds = reviewTimeSeconds; }

    public int getScheduledDays() { return scheduledDays; }
    public void setScheduledDays(int scheduledDays) { this.scheduledDays = scheduledDays; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public Instant getReviewedAt() { return reviewedAt; }
}

