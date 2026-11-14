package com.csscholar.backend.learning.dto;

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

    public int getQuizAttempts() { return quizAttempts; }
    public void setQuizAttempts(int quizAttempts) { this.quizAttempts = quizAttempts; }

    public int getFlashcardReviews() { return flashcardReviews; }
    public void setFlashcardReviews(int flashcardReviews) { this.flashcardReviews = flashcardReviews; }

    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
}

