package com.csscholar.backend.knowledge.exception;

import com.csscholar.backend.common.BadRequestException;

public class KnowledgeGraphIntegrityException extends BadRequestException {

    public KnowledgeGraphIntegrityException(String message) {
        super(message);
    }
}
