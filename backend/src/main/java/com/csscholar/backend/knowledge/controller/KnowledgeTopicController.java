package com.csscholar.backend.knowledge.controller;

import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicDetailResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicSummaryResponse;
import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.TopicType;
import com.csscholar.backend.knowledge.service.KnowledgeTopicService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge/topics")
public class KnowledgeTopicController {

    private final KnowledgeTopicService topicService;

    public KnowledgeTopicController(KnowledgeTopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<KnowledgeTopicSummaryResponse>>> getTopics(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) TopicType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by("importanceScore").descending());

        Page<KnowledgeTopicSummaryResponse> results = topicService.getTopics(q, subjectId, difficulty, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Topics retrieved successfully.", results));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResponse<KnowledgeTopicDetailResponse>> getTopicDetail(@PathVariable String idOrSlug) {
        KnowledgeTopicDetailResponse detail = topicService.getTopicDetail(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Topic detail retrieved successfully.", detail));
    }

    @GetMapping("/{idOrSlug}/prerequisites")
    public ResponseEntity<ApiResponse<List<KnowledgeTopicSummaryResponse>>> getPrerequisites(@PathVariable String idOrSlug) {
        List<KnowledgeTopicSummaryResponse> prerequisites = topicService.getPrerequisites(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Prerequisite topics retrieved.", prerequisites));
    }

    @GetMapping("/{idOrSlug}/related")
    public ResponseEntity<ApiResponse<List<KnowledgeTopicSummaryResponse>>> getRelatedTopics(@PathVariable String idOrSlug) {
        List<KnowledgeTopicSummaryResponse> related = topicService.getRelatedTopics(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Related topics retrieved.", related));
    }

    @GetMapping("/{idOrSlug}/subtopics")
    public ResponseEntity<ApiResponse<List<KnowledgeTopicSummaryResponse>>> getSubtopics(@PathVariable String idOrSlug) {
        List<KnowledgeTopicSummaryResponse> subtopics = topicService.getSubtopics(idOrSlug);
        return ResponseEntity.ok(ApiResponse.success("Subtopics retrieved.", subtopics));
    }
}

