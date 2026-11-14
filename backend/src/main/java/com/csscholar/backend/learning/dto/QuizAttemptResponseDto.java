package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.AttemptStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizAttemptResponseDto {

    private UUID id;
    private UUID quizId;
    private String quizTitle;
    private AttemptStatus status;
    private Double score;
    private int totalQuestions;
    private int correctAnswersCount;
    private Integer timeSpentSeconds;
    private Instant startedAt;
    private Instant submittedAt;
    private List<SavedAnswerDto> answers = new ArrayList<>();

    public QuizAttemptResponseDto() {}

    public QuizAttemptResponseDto(UUID id, UUID quizId, String quizTitle, AttemptStatus status,
                                  Double score, int totalQuestions, int correctAnswersCount,
                                  Integer timeSpentSeconds, Instant startedAt, Instant submittedAt,
                                  List<SavedAnswerDto> answers) {
        this.id = id;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.status = status;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.correctAnswersCount = correctAnswersCount;
        this.timeSpentSeconds = timeSpentSeconds;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.answers = answers;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getQuizId() { return quizId; }
    public void setQuizId(UUID quizId) { this.quizId = quizId; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }

    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }

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

    public List<SavedAnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<SavedAnswerDto> answers) { this.answers = answers; }

    public static class SavedAnswerDto {
        private UUID questionId;
        private UUID selectedOptionId;
        private String selectedText;
        private boolean markedForReview;

        public SavedAnswerDto() {}

        public SavedAnswerDto(UUID questionId, UUID selectedOptionId, String selectedText, boolean markedForReview) {
            this.questionId = questionId;
            this.selectedOptionId = selectedOptionId;
            this.selectedText = selectedText;
            this.markedForReview = markedForReview;
        }

        public UUID getQuestionId() { return questionId; }
        public void setQuestionId(UUID questionId) { this.questionId = questionId; }

        public UUID getSelectedOptionId() { return selectedOptionId; }
        public void setSelectedOptionId(UUID selectedOptionId) { this.selectedOptionId = selectedOptionId; }

        public String getSelectedText() { return selectedText; }
        public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

        public boolean isMarkedForReview() { return markedForReview; }
        public void setMarkedForReview(boolean markedForReview) { this.markedForReview = markedForReview; }
    }
}

