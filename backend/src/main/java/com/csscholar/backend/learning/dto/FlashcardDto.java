package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.ReviewState;

import java.time.Instant;
import java.util.UUID;

public class FlashcardDto {

    private UUID id;
    private UUID deckId;
    private String front;
    private String back;
    private String topicName;
    private String sourceReference;
    private Integer pageNumber;
    private Integer slideNumber;
    private ReviewState state;
    private int repetitionCount;
    private double easeFactor;
    private int intervalDays;
    private Instant nextDueAt;
    private Instant lastReviewedAt;
    private Instant createdAt;

    public FlashcardDto() {}

    public FlashcardDto(UUID id, UUID deckId, String front, String back, String topicName,
                        String sourceReference, Integer pageNumber, Integer slideNumber,
                        ReviewState state, int repetitionCount, double easeFactor,
                        int intervalDays, Instant nextDueAt, Instant lastReviewedAt, Instant createdAt) {
        this.id = id;
        this.deckId = deckId;
        this.front = front;
        this.back = back;
        this.topicName = topicName;
        this.sourceReference = sourceReference;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
        this.state = state;
        this.repetitionCount = repetitionCount;
        this.easeFactor = easeFactor;
        this.intervalDays = intervalDays;
        this.nextDueAt = nextDueAt;
        this.lastReviewedAt = lastReviewedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDeckId() { return deckId; }
    public void setDeckId(UUID deckId) { this.deckId = deckId; }

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

    public ReviewState getState() { return state; }
    public void setState(ReviewState state) { this.state = state; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }

    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

