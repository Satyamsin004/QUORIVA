package com.csscholar.backend.learning.dto;

import java.util.ArrayList;
import java.util.List;

public class LearningAnalyticsOverviewDto {

    private long totalQuizAttempts;
    private double averageQuizScore;
    private long totalDecks;
    private long totalCards;
    private long cardsDueToday;
    private long cardsReviewedCount;
    private long newCardsCount;
    private long learningCardsCount;
    private long reviewCardsCount;
    private long graduatedCardsCount;
    private List<QuizAttemptResponseDto> recentQuizAttempts = new ArrayList<>();
    private List<LearningSessionDto> recentSessions = new ArrayList<>();

    public LearningAnalyticsOverviewDto() {}

    public long getTotalQuizAttempts() { return totalQuizAttempts; }
    public void setTotalQuizAttempts(long totalQuizAttempts) { this.totalQuizAttempts = totalQuizAttempts; }

    public double getAverageQuizScore() { return averageQuizScore; }
    public void setAverageQuizScore(double averageQuizScore) { this.averageQuizScore = averageQuizScore; }

    public long getTotalDecks() { return totalDecks; }
    public void setTotalDecks(long totalDecks) { this.totalDecks = totalDecks; }

    public long getTotalCards() { return totalCards; }
    public void setTotalCards(long totalCards) { this.totalCards = totalCards; }

    public long getCardsDueToday() { return cardsDueToday; }
    public void setCardsDueToday(long cardsDueToday) { this.cardsDueToday = cardsDueToday; }

    public long getCardsReviewedCount() { return cardsReviewedCount; }
    public void setCardsReviewedCount(long cardsReviewedCount) { this.cardsReviewedCount = cardsReviewedCount; }

    public long getNewCardsCount() { return newCardsCount; }
    public void setNewCardsCount(long newCardsCount) { this.newCardsCount = newCardsCount; }

    public long getLearningCardsCount() { return learningCardsCount; }
    public void setLearningCardsCount(long learningCardsCount) { this.learningCardsCount = learningCardsCount; }

    public long getReviewCardsCount() { return reviewCardsCount; }
    public void setReviewCardsCount(long reviewCardsCount) { this.reviewCardsCount = reviewCardsCount; }

    public long getGraduatedCardsCount() { return graduatedCardsCount; }
    public void setGraduatedCardsCount(long graduatedCardsCount) { this.graduatedCardsCount = graduatedCardsCount; }

    public List<QuizAttemptResponseDto> getRecentQuizAttempts() { return recentQuizAttempts; }
    public void setRecentQuizAttempts(List<QuizAttemptResponseDto> recentQuizAttempts) { this.recentQuizAttempts = recentQuizAttempts; }

    public List<LearningSessionDto> getRecentSessions() { return recentSessions; }
    public void setRecentSessions(List<LearningSessionDto> recentSessions) { this.recentSessions = recentSessions; }
}

