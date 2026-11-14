package com.csscholar.backend.library.dto;

public class ModerationActionRequest {

    private String reason;
    private String notes;

    public ModerationActionRequest() {
    }

    public ModerationActionRequest(String reason, String notes) {
        this.reason = reason;
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

