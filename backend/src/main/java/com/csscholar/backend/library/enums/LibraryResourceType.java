package com.csscholar.backend.library.enums;

public enum LibraryResourceType {
    LECTURE_NOTES("Lecture Notes"),
    TEXTBOOK("Textbook / Reference Book"),
    PRESENTATION("Slide Presentation"),
    LAB_MANUAL("Lab Manual / Experiment Guide"),
    ASSIGNMENT("Assignment / Problem Set"),
    PREVIOUS_EXAM_PAPER("Previous Exam Paper / Solutions"),
    RESEARCH_PAPER("Research Paper / Survey"),
    TUTORIAL("Tutorial / Cheat Sheet"),
    REFERENCE_GUIDE("Reference Guide"),
    OTHER("Other Resource");

    private final String displayName;

    LibraryResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

