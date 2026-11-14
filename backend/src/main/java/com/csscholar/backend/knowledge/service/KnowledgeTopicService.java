package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicDetailResponse;
import com.csscholar.backend.knowledge.dto.KnowledgeTopicSummaryResponse;
import com.csscholar.backend.knowledge.dto.TopicResourceLinkResponse;
import com.csscholar.backend.knowledge.entity.*;
import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.RelationshipType;
import com.csscholar.backend.knowledge.enums.TopicStatus;
import com.csscholar.backend.knowledge.enums.TopicType;
import com.csscholar.backend.knowledge.repository.*;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class KnowledgeTopicService {

    private final KnowledgeTopicRepository topicRepository;
    private final KnowledgeTopicRelationshipRepository relationshipRepository;
    private final KnowledgeTopicLearningObjectiveRepository objectiveRepository;
    private final KnowledgeTopicTagRepository tagRepository;
    private final KnowledgeTopicResourceRepository resourceLinkRepository;

    public KnowledgeTopicService(KnowledgeTopicRepository topicRepository,
                                 KnowledgeTopicRelationshipRepository relationshipRepository,
                                 KnowledgeTopicLearningObjectiveRepository objectiveRepository,
                                 KnowledgeTopicTagRepository tagRepository,
                                 KnowledgeTopicResourceRepository resourceLinkRepository) {
        this.topicRepository = topicRepository;
        this.relationshipRepository = relationshipRepository;
        this.objectiveRepository = objectiveRepository;
        this.tagRepository = tagRepository;
        this.resourceLinkRepository = resourceLinkRepository;
    }

    public Page<KnowledgeTopicSummaryResponse> getTopics(String query,
                                                         UUID subjectId,
                                                         DifficultyLevel difficulty,
                                                         TopicType topicType,
                                                         Pageable pageable) {
        Specification<KnowledgeTopic> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), TopicStatus.PUBLISHED));

            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficultyLevel"), difficulty));
            }
            if (topicType != null) {
                predicates.add(cb.equal(root.get("topicType"), topicType));
            }
            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate summaryLike = cb.like(cb.lower(root.get("summary")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, summaryLike, descLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return topicRepository.findAll(spec, pageable).map(this::toSummaryResponse);
    }

    public KnowledgeTopic resolveTopic(String idOrSlug) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return topicRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + idOrSlug));
        } catch (IllegalArgumentException e) {
            return topicRepository.findBySlug(idOrSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found with slug: " + idOrSlug));
        }
    }

    public KnowledgeTopicDetailResponse getTopicDetail(String idOrSlug) {
        KnowledgeTopic topic = resolveTopic(idOrSlug);

        // Learning Objectives
        List<String> objectives = objectiveRepository.findByTopicIdOrderByDisplayOrderAsc(topic.getId())
                .stream()
                .map(KnowledgeTopicLearningObjective::getObjective)
                .toList();

        // Tags
        List<String> tags = tagRepository.findByTopicId(topic.getId())
                .stream()
                .map(KnowledgeTopicTag::getTag)
                .toList();

        // Prerequisites: relationships where target = topic and type = PREREQUISITE_OF
        List<KnowledgeTopicRelationship> prereqRels =
                relationshipRepository.findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF);
        List<KnowledgeTopicSummaryResponse> prerequisites = prereqRels.stream()
                .map(r -> toSummaryResponse(r.getSourceTopic()))
                .toList();

        // Unlocked topics: relationships where source = topic and type = PREREQUISITE_OF
        List<KnowledgeTopicRelationship> unlockedRels =
                relationshipRepository.findBySourceTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF);
        List<KnowledgeTopicSummaryResponse> unlockedTopics = unlockedRels.stream()
                .map(r -> toSummaryResponse(r.getTargetTopic()))
                .toList();

        // Related topics: RELATED_TO in either direction
        List<KnowledgeTopicRelationship> relatedRels =
                relationshipRepository.findRelatedRelationships(topic.getId(), RelationshipType.RELATED_TO);
        List<KnowledgeTopicSummaryResponse> relatedTopics = relatedRels.stream()
                .map(r -> {
                    KnowledgeTopic other = r.getSourceTopic().getId().equals(topic.getId())
                            ? r.getTargetTopic()
                            : r.getSourceTopic();
                    return toSummaryResponse(other);
                })
                .distinct()
                .toList();

        // Subtopics: child topics or PART_OF relationships
        List<KnowledgeTopic> childTopics = topicRepository.findByParentTopicId(topic.getId());
        List<KnowledgeTopicRelationship> partOfRels =
                relationshipRepository.findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PART_OF);
        Set<UUID> subtopicIds = new HashSet<>();
        List<KnowledgeTopicSummaryResponse> subtopics = new ArrayList<>();

        for (KnowledgeTopic child : childTopics) {
            if (subtopicIds.add(child.getId())) {
                subtopics.add(toSummaryResponse(child));
            }
        }
        for (KnowledgeTopicRelationship r : partOfRels) {
            if (subtopicIds.add(r.getSourceTopic().getId())) {
                subtopics.add(toSummaryResponse(r.getSourceTopic()));
            }
        }

        // Published Library Resources (Strict security: only PUBLISHED status, never private workspace docs)
        List<KnowledgeTopicResource> linkedResources =
                resourceLinkRepository.findByTopicIdAndResourceStatus(topic.getId(), LibraryResourceStatus.PUBLISHED);
        List<TopicResourceLinkResponse> publishedResources = linkedResources.stream()
                .map(lr -> new TopicResourceLinkResponse(
                        lr.getResource().getId(),
                        lr.getResource().getTitle(),
                        lr.getResource().getResourceType().name(),
                        lr.getResource().getFileType(),
                        lr.getResource().getLicenseName(),
                        lr.getRelevanceNotes()
                ))
                .toList();

        return new KnowledgeTopicDetailResponse(
                topic.getId(),
                topic.getSubject().getId(),
                topic.getSubject().getName(),
                topic.getSubject().getSlug(),
                topic.getParentTopic() != null ? topic.getParentTopic().getId() : null,
                topic.getParentTopic() != null ? topic.getParentTopic().getName() : null,
                topic.getName(),
                topic.getSlug(),
                topic.getSummary(),
                topic.getDescription(),
                topic.getDifficultyLevel(),
                topic.getTopicType(),
                topic.getEstimatedMinutes(),
                objectives,
                tags,
                prerequisites,
                unlockedTopics,
                relatedTopics,
                subtopics,
                publishedResources,
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }

    public List<KnowledgeTopicSummaryResponse> getPrerequisites(String idOrSlug) {
        KnowledgeTopic topic = resolveTopic(idOrSlug);
        return relationshipRepository.findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF)
                .stream()
                .map(r -> toSummaryResponse(r.getSourceTopic()))
                .toList();
    }

    public List<KnowledgeTopicSummaryResponse> getRelatedTopics(String idOrSlug) {
        KnowledgeTopic topic = resolveTopic(idOrSlug);
        return relationshipRepository.findRelatedRelationships(topic.getId(), RelationshipType.RELATED_TO)
                .stream()
                .map(r -> {
                    KnowledgeTopic other = r.getSourceTopic().getId().equals(topic.getId())
                            ? r.getTargetTopic()
                            : r.getSourceTopic();
                    return toSummaryResponse(other);
                })
                .distinct()
                .toList();
    }

    public List<KnowledgeTopicSummaryResponse> getSubtopics(String idOrSlug) {
        KnowledgeTopic topic = resolveTopic(idOrSlug);
        List<KnowledgeTopic> children = topicRepository.findByParentTopicId(topic.getId());
        List<KnowledgeTopicRelationship> partOfRels =
                relationshipRepository.findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PART_OF);

        Set<UUID> ids = new HashSet<>();
        List<KnowledgeTopicSummaryResponse> list = new ArrayList<>();
        for (KnowledgeTopic child : children) {
            if (ids.add(child.getId())) {
                list.add(toSummaryResponse(child));
            }
        }
        for (KnowledgeTopicRelationship r : partOfRels) {
            if (ids.add(r.getSourceTopic().getId())) {
                list.add(toSummaryResponse(r.getSourceTopic()));
            }
        }
        return list;
    }

    public KnowledgeTopicSummaryResponse toSummaryResponse(KnowledgeTopic topic) {
        List<String> tags = tagRepository.findByTopicId(topic.getId())
                .stream()
                .map(KnowledgeTopicTag::getTag)
                .toList();

        int prereqCount = relationshipRepository
                .findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF).size();
        int unlockedCount = relationshipRepository
                .findBySourceTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF).size();

        return new KnowledgeTopicSummaryResponse(
                topic.getId(),
                topic.getSubject().getId(),
                topic.getSubject().getName(),
                topic.getSubject().getSlug(),
                topic.getName(),
                topic.getSlug(),
                topic.getSummary(),
                topic.getDifficultyLevel(),
                topic.getTopicType(),
                topic.getEstimatedMinutes(),
                tags,
                prereqCount,
                unlockedCount
        );
    }
}

