package com.csscholar.backend.learning.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizAttemptReviewDto {

    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private Double score;
    private int totalQuestions;
    private int correctAnswersCount;
    private Integer timeSpentSeconds;
    private Instant startedAt;
    private Instant submittedAt;
    private List<QuizQuestionReviewDto> questions = new ArrayList<>();

    public QuizAttemptReviewDto() {}

    public QuizAttemptReviewDto(UUID attemptId, UUID quizId, String quizTitle, Double score,
                                int totalQuestions, int correctAnswersCount, Integer timeSpentSeconds,
                                Instant startedAt, Instant submittedAt, List<QuizQuestionReviewDto> questions) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.correctAnswersCount = correctAnswersCount;
        this.timeSpentSeconds = timeSpentSeconds;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.questions = questions;
    }

    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }

    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectAnswersCount() { return correctAnswersCount; }
    public void setCorrectAnswersCount(int correctAnswersCount) { this.correctAnswersCount = correctAnswersCount; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public List<QuizQuestionReviewDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionReviewDto> questions) { this.questions = questions; }
}

