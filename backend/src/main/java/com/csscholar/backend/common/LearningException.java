package com.csscholar.backend.common;

public class LearningException extends RuntimeException {

    public LearningException(String message) {
        super(message);
    }

    public LearningException(String message, Throwable cause) {
        super(message, cause);
    }
}

