package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.knowledge.dto.AcademicSubjectResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicSummaryResponse;
import com.csscholar.backend.knowledge.entity.AcademicSubject;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.repository.AcademicSubjectRepository;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class KnowledgeSubjectService {

    private final AcademicSubjectRepository subjectRepository;
    private final KnowledgeTopicRepository topicRepository;
    private final KnowledgeTopicService topicService;

    public KnowledgeSubjectService(AcademicSubjectRepository subjectRepository,
                                  KnowledgeTopicRepository topicRepository,
                                  KnowledgeTopicService topicService) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
    }

    public List<AcademicSubjectResponse> getAllSubjects() {
        return subjectRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toSubjectResponse)
                .toList();
    }

    public AcademicSubjectResponse getSubjectById(UUID subjectId) {
        AcademicSubject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        return toSubjectResponse(subject);
    }

    public AcademicSubjectResponse getSubjectBySlug(String slug) {
        AcademicSubject subject = subjectRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with slug: " + slug));
        return toSubjectResponse(subject);
    }

    public List<KnowledgeTopicSummaryResponse> getTopicsBySubject(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found: " + subjectId);
        }
        List<KnowledgeTopic> topics = topicRepository.findBySubjectIdOrderByImportanceScoreDesc(subjectId);
        return topics.stream()
                .map(topicService::toSummaryResponse)
                .toList();
    }

    public AcademicSubjectResponse toSubjectResponse(AcademicSubject s) {
        long count = topicRepository.findBySubjectIdOrderByImportanceScoreDesc(s.getId()).size();
        return new AcademicSubjectResponse(
                s.getId(),
                s.getName(),
                s.getSlug(),
                s.getCode(),
                s.getDescription(),
                s.getCategory(),
                s.getIconName(),
                s.getDisplayOrder(),
                count,
                s.getCreatedAt()
        );
    }
}

