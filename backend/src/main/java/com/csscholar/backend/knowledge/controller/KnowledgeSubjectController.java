package com.csscholar.backend.knowledge.controller;

import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.knowledge.dto.AcademicSubjectResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicSummaryResponse;
import com.csscholar.backend.knowledge.service.KnowledgeSubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge/subjects")
public class KnowledgeSubjectController {

    private final KnowledgeSubjectService subjectService;

    public KnowledgeSubjectController(KnowledgeSubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AcademicSubjectResponse>>> getAllSubjects() {
        List<AcademicSubjectResponse> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved successfully.", subjects));
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<AcademicSubjectResponse>> getSubjectById(@PathVariable UUID subjectId) {
        AcademicSubjectResponse subject = subjectService.getSubjectById(subjectId);
        return ResponseEntity.ok(ApiResponse.success("Subject details retrieved.", subject));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<AcademicSubjectResponse>> getSubjectBySlug(@PathVariable String slug) {
        AcademicSubjectResponse subject = subjectService.getSubjectBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Subject details retrieved.", subject));
    }

    @GetMapping("/{subjectId}/topics")
    public ResponseEntity<ApiResponse<List<KnowledgeTopicSummaryResponse>>> getTopicsBySubject(@PathVariable UUID subjectId) {
        List<KnowledgeTopicSummaryResponse> topics = subjectService.getTopicsBySubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success("Subject topics retrieved.", topics));
    }
}

