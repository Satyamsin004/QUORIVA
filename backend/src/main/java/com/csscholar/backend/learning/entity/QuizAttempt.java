package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.AttemptStatus;
import com.csscholar.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column
    private Double score;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "correct_answers_count", nullable = false)
    private int correctAnswersCount = 0;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttemptAnswer> answers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    public QuizAttempt() {}

    public QuizAttempt(Quiz quiz, User user, int totalQuestions) {
        this.quiz = quiz;
        this.user = user;
        this.totalQuestions = totalQuestions;
        this.status = AttemptStatus.IN_PROGRESS;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public List<QuizAttemptAnswer> getAnswers() { return answers; }
    public void setAnswers(List<QuizAttemptAnswer> answers) { this.answers = answers; }

    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}

