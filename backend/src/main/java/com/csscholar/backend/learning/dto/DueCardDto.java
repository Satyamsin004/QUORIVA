package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.ReviewState;

import java.util.UUID;

public class DueCardDto {

    private UUID id;
    private UUID deckId;
    private String deckTitle;
    private String front;
    private String back;
    private String topicName;
    private String sourceReference;
    private ReviewState state;
    private int intervalDays;
    private double easeFactor;
    private int repetitionCount;

    public DueCardDto() {}

    public DueCardDto(UUID id, UUID deckId, String deckTitle, String front, String back,
                      String topicName, String sourceReference, ReviewState state,
                      int intervalDays, double easeFactor, int repetitionCount) {
        this.id = id;
        this.deckId = deckId;
        this.deckTitle = deckTitle;
        this.front = front;
        this.back = back;
        this.topicName = topicName;
        this.sourceReference = sourceReference;
        this.state = state;
        this.intervalDays = intervalDays;
        this.easeFactor = easeFactor;
        this.repetitionCount = repetitionCount;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDeckId() { return deckId; }
    public void setDeckId(UUID deckId) { this.deckId = deckId; }

    public String getDeckTitle() { return deckTitle; }
    public void setDeckTitle(String deckTitle) { this.deckTitle = deckTitle; }

    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }

    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public ReviewState getState() { return state; }
    public void setState(ReviewState state) { this.state = state; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public int getRepetitionCount() { return repetitionCount; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }
}

