package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuizQuestionReviewDto {

    private UUID questionId;
    private int questionIndex;
    private String questionText;
    private QuestionType questionType;
    private String explanation;
    private String sourceReference;
    private Integer pageNumber;
    private Integer slideNumber;
    private String topicName;

    @JsonProperty("isCorrect")
    @JsonAlias({"is_correct", "correct", "isCorrect"})
    private Boolean isCorrect;

    private UUID selectedOptionId;
    private UUID correctOptionId;
    private List<QuizOptionReviewDto> options = new ArrayList<>();

    public QuizQuestionReviewDto() {}

    public QuizQuestionReviewDto(UUID questionId, int questionIndex, String questionText,
                                 QuestionType questionType, String explanation, String sourceReference,
                                 Integer pageNumber, Integer slideNumber, String topicName,
                                 Boolean isCorrect, UUID selectedOptionId, UUID correctOptionId,
                                 List<QuizOptionReviewDto> options) {
        this.questionId = questionId;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.questionType = questionType;
        this.explanation = explanation;
        this.sourceReference = sourceReference;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
        this.topicName = topicName;
        this.isCorrect = isCorrect;
        this.selectedOptionId = selectedOptionId;
        this.correctOptionId = correctOptionId;
        this.options = options;
    }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

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

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public UUID getSelectedOptionId() { return selectedOptionId; }
    public void setSelectedOptionId(UUID selectedOptionId) { this.selectedOptionId = selectedOptionId; }

    public UUID getCorrectOptionId() { return correctOptionId; }
    public void setCorrectOptionId(UUID correctOptionId) { this.correctOptionId = correctOptionId; }

    public List<QuizOptionReviewDto> getOptions() { return options; }
    public void setOptions(List<QuizOptionReviewDto> options) { this.options = options; }
}

