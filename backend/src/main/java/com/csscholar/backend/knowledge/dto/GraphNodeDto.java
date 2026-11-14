package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.TopicType;

import java.util.UUID;

public record GraphNodeDto(
        UUID id,
        String name,
        String slug,
        UUID subjectId,
        String subjectName,
        TopicType topicType,
        DifficultyLevel difficultyLevel,
        double importanceScore,
        int prerequisiteCount,
        int unlockedCount,
        boolean isCenter
) {}

