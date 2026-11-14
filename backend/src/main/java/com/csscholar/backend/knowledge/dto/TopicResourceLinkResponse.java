package com.csscholar.backend.knowledge.dto;

import java.util.UUID;

public record TopicResourceLinkResponse(
        UUID resourceId,
        String title,
        String resourceType,
        String fileType,
        String licenseName,
        String relevanceNotes
) {}

