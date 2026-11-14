package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.LearningDifficultyLevel;
import com.csscholar.backend.learning.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Question DTO presented during practice taking.
 * Correct answers and explanations are strictly omitted to prevent client inspection.
 */
public class QuizQuestionPracticeDto {

    private UUID id;
    private int questionIndex;
    private String questionText;
    private QuestionType questionType;
    private LearningDifficultyLevel difficultyLevel;
    private String sourceReference;
    private Integer pageNumber;
    private Integer slideNumber;
    private String topicName;
    private List<QuizOptionPracticeDto> options = new ArrayList<>();

    public QuizQuestionPracticeDto() {}

    public QuizQuestionPracticeDto(UUID id, int questionIndex, String questionText,
                                   QuestionType questionType, LearningDifficultyLevel difficultyLevel,
                                   String sourceReference, Integer pageNumber, Integer slideNumber,
                                   String topicName, List<QuizOptionPracticeDto> options) {
        this.id = id;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficultyLevel = difficultyLevel;
        this.sourceReference = sourceReference;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
        this.topicName = topicName;
        this.options = options;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public LearningDifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(LearningDifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getSlideNumber() { return slideNumber; }
    public void setSlideNumber(Integer slideNumber) { this.slideNumber = slideNumber; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public List<QuizOptionPracticeDto> getOptions() { return options; }
    public void setOptions(List<QuizOptionPracticeDto> options) { this.options = options; }
}

