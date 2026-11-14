package com.csscholar.backend.knowledge.controller;

import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.knowledge.dto.LearningPathResponse;
import com.csscholar.backend.knowledge.service.KnowledgePathService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgePathController {

    private final KnowledgePathService pathService;

    public KnowledgePathController(KnowledgePathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping("/topics/{idOrSlug}/learning-path")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getLearningPath(@PathVariable String idOrSlug) {
        LearningPathResponse path = pathService.generateLearningPath(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Learning path generated successfully.", path));
    }

    @GetMapping("/learning-paths")
    public ResponseEntity<ApiResponse<List<LearningPathResponse>>> getCuratedLearningPaths() {
        List<LearningPathResponse> curated = pathService.getCuratedLearningPaths();
        return ResponseEntity.ok(ApiResponse.success("Curated learning paths retrieved.", curated));
    }
}

