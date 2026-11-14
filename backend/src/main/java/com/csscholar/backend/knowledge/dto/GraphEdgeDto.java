package com.csscholar.backend.knowledge.dto;

import com.csscholar.backend.knowledge.enums.RelationshipType;

import java.util.UUID;

public record GraphEdgeDto(
        UUID id,
        UUID source,
        UUID target,
        RelationshipType relationshipType,
        String description,
        boolean isBidirectional
) {}

