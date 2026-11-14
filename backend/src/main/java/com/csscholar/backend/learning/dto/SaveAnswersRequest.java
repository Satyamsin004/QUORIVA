package com.csscholar.backend.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SaveAnswersRequest {

    @Valid
    private List<AnswerItemDto> answers = new ArrayList<>();

    private Integer timeSpentSeconds;

    public SaveAnswersRequest() {}

    public List<AnswerItemDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerItemDto> answers) { this.answers = answers; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public static class AnswerItemDto {
        @NotNull
        private UUID questionId;
        private UUID selectedOptionId;
        private String selectedText;
        private boolean markedForReview;

        public AnswerItemDto() {}

        public AnswerItemDto(UUID questionId, UUID selectedOptionId, String selectedText, boolean markedForReview) {
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

