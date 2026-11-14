package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.RelationshipType;

import java.util.UUID;

public record KnowledgeRelationshipResponse(
        UUID relationshipId,
        UUID sourceTopicId,
        String sourceTopicName,
        String sourceTopicSlug,
        UUID targetTopicId,
        String targetTopicName,
        String targetTopicSlug,
        RelationshipType relationshipType,
        String description,
        boolean isBidirectional
) {}

