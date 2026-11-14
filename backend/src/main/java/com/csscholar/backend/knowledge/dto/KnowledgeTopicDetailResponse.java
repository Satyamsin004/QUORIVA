package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.TopicType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeTopicDetailResponse(
        UUID id,
        UUID subjectId,
        String subjectName,
        String subjectSlug,
        UUID parentTopicId,
        String parentTopicName,
        String name,
        String slug,
        String summary,
        String description,
        DifficultyLevel difficultyLevel,
        TopicType topicType,
        int estimatedMinutes,
        List<String> learningObjectives,
        List<String> tags,
        List<KnowledgeTopicSummaryResponse> prerequisites,
        List<KnowledgeTopicSummaryResponse> unlockedTopics,
        List<KnowledgeTopicSummaryResponse> relatedTopics,
        List<KnowledgeTopicSummaryResponse> subtopics,
        List<TopicResourceLinkResponse> publishedResources,
        Instant createdAt,
        Instant updatedAt
) {}

