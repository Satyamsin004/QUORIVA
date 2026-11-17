package com.csscholar.backend.document.service;

import java.util.Locale;

/** Keeps broad legacy labels out of the material collections when a filename is clear. */
final class SubjectClassifier {
    private SubjectClassifier() {}

    static String inferFromFilename(String filename) {
        String value = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (value.contains("javascript") || value.contains("java-script") || value.startsWith("js_")) return "JavaScript";
        if (value.contains("python") || value.contains("pyth")) return "Python";
        if (value.contains("java")) return "Java";
        if (value.contains("data structure") || value.contains("algorithm") || value.contains("dsa")) return "Data Structures & Algorithms";
        if (value.contains("database") || value.contains("dbms") || value.contains("sql")) return "Database Systems (DBMS)";
        if (value.contains("network")) return "Computer Networks";
        if (value.contains("operating system")) return "Operating Systems";
        return "General";
    }

    static String resolve(String requestedSubject, String filename) {
        if (requestedSubject == null || requestedSubject.isBlank()
                || requestedSubject.trim().equalsIgnoreCase("computer science")) return inferFromFilename(filename);
        return requestedSubject.trim();
    }
}
