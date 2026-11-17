package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DailyActivityDto {

    private String date;
    private int quizAttempts;
    private int flashcardReviews;
    private int totalMinutes;

    public DailyActivityDto() {}

    public DailyActivityDto(String date, int quizAttempts, int flashcardReviews, int totalMinutes) {
        this.date = date;
        this.quizAttempts = quizAttempts;
        this.flashcardReviews = flashcardReviews;
        this.totalMinutes = totalMinutes;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @JsonProperty("quizAttempts")
    public int getQuizAttempts() { return quizAttempts; }
    public void setQuizAttempts(int quizAttempts) { this.quizAttempts = quizAttempts; }

    @JsonProperty("quizzesTaken")
    public int getQuizzesTaken() { return quizAttempts; }

    @JsonProperty("flashcardReviews")
    public int getFlashcardReviews() { return flashcardReviews; }
    public void setFlashcardReviews(int flashcardReviews) { this.flashcardReviews = flashcardReviews; }

    @JsonProperty("cardsReviewed")
    public int getCardsReviewed() { return flashcardReviews; }

    @JsonProperty("totalMinutes")
    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }

    @JsonProperty("studyTimeMinutes")
    public int getStudyTimeMinutes() { return totalMinutes; }
}

