package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReportResolutionRequest {

    @NotNull(message = "Resolution action is required (RESOLVED or DISMISSED)")
    private ReportStatus action;

    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;

    public ReportResolutionRequest() {
    }

    public ReportResolutionRequest(ReportStatus action, String resolutionNotes) {
        this.action = action;
        this.resolutionNotes = resolutionNotes;
    }

    public ReportStatus getAction() {
        return action;
    }

    public void setAction(ReportStatus action) {
        this.action = action;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}

