package com.csscholar.backend.learning.dto;

public class TopicPerformanceDto {

    private String topicName;
    private int quizzesTaken;
    private double averageScore;
    private int totalCards;
    private int masteredCards;

    public TopicPerformanceDto() {}

    public TopicPerformanceDto(String topicName, int quizzesTaken, double averageScore, int totalCards, int masteredCards) {
        this.topicName = topicName;
        this.quizzesTaken = quizzesTaken;
        this.averageScore = averageScore;
        this.totalCards = totalCards;
        this.masteredCards = masteredCards;
    }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public int getQuizzesTaken() { return quizzesTaken; }
    public void setQuizzesTaken(int quizzesTaken) { this.quizzesTaken = quizzesTaken; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public int getTotalCards() { return totalCards; }
    public void setTotalCards(int totalCards) { this.totalCards = totalCards; }

    public int getMasteredCards() { return masteredCards; }
    public void setMasteredCards(int masteredCards) { this.masteredCards = masteredCards; }
}

