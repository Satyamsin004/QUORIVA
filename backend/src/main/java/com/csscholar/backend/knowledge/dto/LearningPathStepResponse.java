package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.DifficultyLevel;

import java.util.UUID;

public record LearningPathStepResponse(
        int stepNumber,
        UUID topicId,
        String name,
        String slug,
        String subjectName,
        DifficultyLevel difficultyLevel,
        int estimatedMinutes,
        String summary,
        boolean isTargetTopic,
        int directPrerequisitesCount
) {}

