package com.csscholar.backend.library.enums;

public enum ReportReasonCategory {
    COPYRIGHT_INFRINGEMENT("Copyright Infringement / Unauthorized Work"),
    INAPPROPRIATE_CONTENT("Inappropriate or Offensive Material"),
    INCORRECT_ATTRIBUTION("Incorrect Author Attribution / Plagiarism"),
    DEFECTIVE_CORRUPT("Damaged, Corrupt, or Unreadable File"),
    SPAM_MISLEADING("Spam, Advertising, or Misleading Title"),
    OTHER("Other Academic Concern");

    private final String displayName;

    ReportReasonCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

