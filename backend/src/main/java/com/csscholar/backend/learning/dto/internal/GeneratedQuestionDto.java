package com.csscholar.backend.learning.dto.internal;

import java.util.ArrayList;
import java.util.List;

public class GeneratedQuestionDto {

    private int questionIndex;
    private String questionText;
    private String questionType;
    private String difficultyLevel;
    private String explanation;
    private String sourceReference;
    private Integer pageNumber;
    private Integer slideNumber;
    private String topicName;
    private List<GeneratedOptionDto> options = new ArrayList<>();

    public GeneratedQuestionDto() {}

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

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

    public List<GeneratedOptionDto> getOptions() { return options; }
    public void setOptions(List<GeneratedOptionDto> options) { this.options = options; }
}

