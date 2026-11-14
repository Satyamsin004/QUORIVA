package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.TopicType;

import java.util.List;
import java.util.UUID;

public record KnowledgeTopicSummaryResponse(
        UUID id,
        UUID subjectId,
        String subjectName,
        String subjectSlug,
        String name,
        String slug,
        String summary,
        DifficultyLevel difficultyLevel,
        TopicType topicType,
        int estimatedMinutes,
        List<String> tags,
        int prerequisiteCount,
        int unlockedTopicCount
) {}

