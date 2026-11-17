package com.csscholar.backend.learning.dto.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GeneratedOptionDto {

    @JsonProperty("optionIndex")
    @JsonAlias({"option_index", "optionIndex"})
    private int optionIndex;

    @JsonProperty("optionText")
    @JsonAlias({"option_text", "optionText"})
    private String optionText;

    @JsonProperty("isCorrect")
    @JsonAlias({"is_correct", "correct", "isCorrect"})
    private boolean isCorrect;

    public GeneratedOptionDto() {}

    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int optionIndex) { this.optionIndex = optionIndex; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { this.isCorrect = correct; }
}

