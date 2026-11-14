package com.csscholar.backend.library.enums;

public enum AcademicCategory {
    DATA_STRUCTURES("Data Structures"),
    ALGORITHMS("Algorithms"),
    OPERATING_SYSTEMS("Operating Systems"),
    DATABASE_SYSTEMS("Database Management Systems"),
    COMPUTER_NETWORKS("Computer Networks"),
    COMPUTER_ORGANIZATION("Computer Organization & Architecture"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence"),
    MACHINE_LEARNING("Machine Learning"),
    COMPILER_DESIGN("Compiler Design"),
    THEORY_OF_COMPUTATION("Theory of Computation"),
    MATHEMATICS_FOR_CS("Discrete Mathematics & Math for CS"),
    CYBERSECURITY("Cybersecurity & Cryptography"),
    CLOUD_COMPUTING("Cloud Computing & Distributed Systems"),
    WEB_TECHNOLOGIES("Web & Mobile Development"),
    OTHER("Other CS Topics");

    private final String displayName;

    AcademicCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

