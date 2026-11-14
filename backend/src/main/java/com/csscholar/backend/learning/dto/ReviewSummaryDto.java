package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.ReviewRating;
import com.csscholar.backend.learning.enums.ReviewState;

import java.time.Instant;
import java.util.UUID;

public class ReviewSummaryDto {

    private UUID cardId;
    private ReviewRating rating;
    private int scheduledDays;
    private Instant nextDueAt;
    private double easeFactor;
    private int repetitionCount;
    private ReviewState state;

    public ReviewSummaryDto() {}

    public ReviewSummaryDto(UUID cardId, ReviewRating rating, int scheduledDays,
                            Instant nextDueAt, double easeFactor, int repetitionCount, ReviewState state) {
        this.cardId = cardId;
        this.rating = rating;
        this.scheduledDays = scheduledDays;
        this.nextDueAt = nextDueAt;
        this.easeFactor = easeFactor;
        this.repetitionCount = repetitionCount;
        this.state = state;
    }

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }

    public ReviewRating getRating() { return rating; }
    public void setRating(ReviewRating rating) { this.rating = rating; }

    public int getScheduledDays() { return scheduledDays; }
    public void setScheduledDays(int scheduledDays) { this.scheduledDays = scheduledDays; }

    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    public ReviewState getState() { return state; }
    public void setState(ReviewState state) { this.state = state; }
}

