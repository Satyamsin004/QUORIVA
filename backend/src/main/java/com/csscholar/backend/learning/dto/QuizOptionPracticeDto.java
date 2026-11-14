package com.csscholar.backend.learning.dto;

import java.util.UUID;

public class QuizOptionPracticeDto {

    private UUID id;
    private int optionIndex;
    private String optionText;

    public QuizOptionPracticeDto() {}

    public QuizOptionPracticeDto(UUID id, int optionIndex, String optionText) {
        this.id = id;
        this.optionIndex = optionIndex;
        this.optionText = optionText;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int optionIndex) { this.optionIndex = optionIndex; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
}

