package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.ReportReasonCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReportRequest {

    @NotNull(message = "Reason category is required")
    private ReportReasonCategory reasonCategory;

    @NotBlank(message = "Explanation is required")
    @Size(max = 2000, message = "Explanation cannot exceed 2000 characters")
    private String explanation;

    public CreateReportRequest() {
    }

    public CreateReportRequest(ReportReasonCategory reasonCategory, String explanation) {
        this.reasonCategory = reasonCategory;
        this.explanation = explanation;
    }

    public ReportReasonCategory getReasonCategory() {
        return reasonCategory;
    }

    public void setReasonCategory(ReportReasonCategory reasonCategory) {
        this.reasonCategory = reasonCategory;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}

