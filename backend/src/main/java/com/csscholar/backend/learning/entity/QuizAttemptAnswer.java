package com.csscholar.backend.learning.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuizQuestionOption selectedOption;

    @Column(name = "selected_text", columnDefinition = "TEXT")
    private String selectedText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "is_marked_for_review", nullable = false)
    private boolean isMarkedForReview = false;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    public QuizAttemptAnswer() {}

    public QuizAttemptAnswer(QuizAttempt attempt, QuizQuestion question, QuizQuestionOption selectedOption,
                             String selectedText, boolean isMarkedForReview) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.selectedText = selectedText;
        this.isMarkedForReview = isMarkedForReview;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public QuizAttempt getAttempt() { return attempt; }
    public void setAttempt(QuizAttempt attempt) { this.attempt = attempt; }

    public QuizQuestion getQuestion() { return question; }
    public void setQuestion(QuizQuestion question) { this.question = question; }

    public QuizQuestionOption getSelectedOption() { return selectedOption; }
    public void setSelectedOption(QuizQuestionOption selectedOption) { this.selectedOption = selectedOption; }

    public String getSelectedText() { return selectedText; }
    public void setSelectedText(String selectedText) { this.selectedText = selectedText; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public boolean isMarkedForReview() { return isMarkedForReview; }
    public void setMarkedForReview(boolean markedForReview) { isMarkedForReview = markedForReview; }

    public Instant getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(Instant answeredAt) { this.answeredAt = answeredAt; }
}

