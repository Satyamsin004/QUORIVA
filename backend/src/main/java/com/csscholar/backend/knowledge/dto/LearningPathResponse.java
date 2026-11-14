package com.csscholar.backend.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record LearningPathResponse(
        UUID targetTopicId,
        String targetTopicName,
        String targetTopicSlug,
        int totalSteps,
        int estimatedTotalMinutes,
        boolean hasPrerequisites,
        List<LearningPathStepResponse> steps
) {}

