package com.csscholar.backend.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record KnowledgeGraphResponse(
        UUID centerTopicId,
        String centerTopicName,
        int depth,
        int totalNodes,
        int totalEdges,
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {}

