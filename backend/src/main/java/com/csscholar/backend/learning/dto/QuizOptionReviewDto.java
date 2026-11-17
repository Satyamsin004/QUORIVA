package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class QuizOptionReviewDto {

    private UUID id;
    private int optionIndex;
    private String optionText;

    @JsonProperty("isCorrect")
    @JsonAlias({"is_correct", "correct", "isCorrect"})
    private boolean isCorrect;

    public QuizOptionReviewDto() {}

    public QuizOptionReviewDto(UUID id, int optionIndex, String optionText, boolean isCorrect) {
        this.id = id;
        this.optionIndex = optionIndex;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int optionIndex) { this.optionIndex = optionIndex; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { this.isCorrect = correct; }
}

