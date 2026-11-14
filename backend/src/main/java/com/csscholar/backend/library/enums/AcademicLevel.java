package com.csscholar.backend.library.enums;

public enum AcademicLevel {
    UNDERGRADUATE_LOWER("Undergraduate (1st - 2nd Year)"),
    UNDERGRADUATE_UPPER("Undergraduate (3rd - 4th Year)"),
    GRADUATE("Master's / Postgraduate"),
    DOCTORAL("Doctoral / Advanced Research"),
    ALL_LEVELS("All Academic Levels");

    private final String displayName;

    AcademicLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

