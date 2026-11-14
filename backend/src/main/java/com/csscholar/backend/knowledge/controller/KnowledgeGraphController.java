package com.csscholar.backend.knowledge.controller;

import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeGraphResponse;
import com.csscholar.backend.knowledge.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge/graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService graphService;

    public KnowledgeGraphController(KnowledgeGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<KnowledgeGraphResponse>> getFullGraph(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(defaultValue = "50") int maxNodes
    ) {
        KnowledgeGraphResponse graph = graphService.getFullGraph(subjectId, maxNodes);
        return ResponseEntity.ok(ApiResponse.success("Knowledge graph retrieved.", graph));
    }

    @GetMapping("/topics/{idOrSlug}")
    public ResponseEntity<ApiResponse<KnowledgeGraphResponse>> getTopicNeighborhoodGraph(
            @PathVariable String idOrSlug,
            @RequestParam(defaultValue = "1") int depth
    ) {
        KnowledgeGraphResponse graph = graphService.getTopicNeighborhoodGraph(idOrSlug, depth);
        return ResponseEntity.ok(ApiResponse.success("Topic neighborhood graph retrieved.", graph));
    }
}

