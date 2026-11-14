package com.csscholar.backend.learning.entity;

import com.csscholar.backend.learning.enums.LearningDifficultyLevel;
import com.csscholar.backend.learning.enums.QuestionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_index", nullable = false)
    private int questionIndex;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 50)
    private LearningDifficultyLevel difficultyLevel = LearningDifficultyLevel.MEDIUM;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(name = "source_reference", length = 512)
    private String sourceReference;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "slide_number")
    private Integer slideNumber;

    @Column(name = "topic_name", length = 255)
    private String topicName;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionIndex ASC")
    private List<QuizQuestionOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public QuizQuestion() {}

    public QuizQuestion(Quiz quiz, int questionIndex, String questionText, QuestionType questionType,
                        LearningDifficultyLevel difficultyLevel, String explanation,
                        String sourceReference, Integer pageNumber, Integer slideNumber, String topicName) {
        this.quiz = quiz;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficultyLevel = difficultyLevel;
        this.explanation = explanation;
        this.sourceReference = sourceReference;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
        this.topicName = topicName;
    }

    public void addOption(QuizQuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public LearningDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getSlideNumber() { return slideNumber; }
    public void setSlideNumber(Integer slideNumber) { this.slideNumber = slideNumber; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public List<QuizQuestionOption> getOptions() { return options; }
    public void setOptions(List<QuizQuestionOption> options) { this.options = options; }

    public Instant getCreatedAt() { return createdAt; }
}

