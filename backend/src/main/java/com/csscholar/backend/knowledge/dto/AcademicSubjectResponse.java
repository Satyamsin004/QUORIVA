package com.csscholar.backend.knowledge.dto;

import java.time.Instant;
import java.util.UUID;

public record AcademicSubjectResponse(
        UUID id,
        String name,
        String slug,
        String code,
        String description,
        String category,
        String iconName,
        int displayOrder,
        long topicCount,
        Instant createdAt
) {}

