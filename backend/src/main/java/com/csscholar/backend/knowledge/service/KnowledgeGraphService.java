package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.knowledge.dto.GraphEdgeDto;
import com.csscholar.backend.knowledge.dto.GraphNodeDto;
import com.csscholar.backend.knowledge.dto.KnowledgeGraphResponse;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicRelationship;
import com.csscholar.backend.knowledge.enums.RelationshipType;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRelationshipRepository;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class KnowledgeGraphService {

    private final KnowledgeTopicRepository topicRepository;
    private final KnowledgeTopicRelationshipRepository relationshipRepository;
    private final KnowledgeTopicService topicService;

    public KnowledgeGraphService(KnowledgeTopicRepository topicRepository,
                                 KnowledgeTopicRelationshipRepository relationshipRepository,
                                 KnowledgeTopicService topicService) {
        this.topicRepository = topicRepository;
        this.relationshipRepository = relationshipRepository;
        this.topicService = topicService;
    }

    public KnowledgeGraphResponse getFullGraph(UUID subjectId, int maxNodes) {
        int boundedMax = Math.max(10, Math.min(maxNodes, 100));
        List<KnowledgeTopic> topics = topicRepository.findPublishedBySubjectId(subjectId, PageRequest.of(0, boundedMax));

        Set<UUID> topicIds = new HashSet<>();
        for (KnowledgeTopic t : topics) {
            topicIds.add(t.getId());
        }

        List<KnowledgeTopicRelationship> allRels = relationshipRepository.findRelationshipsByTopicIds(topicIds);
        List<GraphEdgeDto> edges = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();

        for (KnowledgeTopicRelationship r : allRels) {
            if (topicIds.contains(r.getSourceTopic().getId()) && topicIds.contains(r.getTargetTopic().getId())) {
                String edgeKey = r.getSourceTopic().getId() + "->" + r.getTargetTopic().getId() + ":" + r.getRelationshipType();
                if (seenEdges.add(edgeKey)) {
                    edges.add(new GraphEdgeDto(
                            r.getId(),
                            r.getSourceTopic().getId(),
                            r.getTargetTopic().getId(),
                            r.getRelationshipType(),
                            r.getDescription(),
                            r.isBidirectional()
                    ));
                }
            }
        }

        List<GraphNodeDto> nodes = topics.stream()
                .map(t -> toGraphNode(t, false))
                .toList();

        return new KnowledgeGraphResponse(
                null,
                null,
                1,
                nodes.size(),
                edges.size(),
                nodes,
                edges
        );
    }

    public KnowledgeGraphResponse getTopicNeighborhoodGraph(String idOrSlug, int depth) {
        KnowledgeTopic centerTopic = topicService.resolveTopic(idOrSlug);
        int boundedDepth = Math.max(1, Math.min(depth, 3));
        int maxAllowedNodes = 60;

        Set<UUID> currentLevelIds = new HashSet<>();
        currentLevelIds.add(centerTopic.getId());

        Set<UUID> allVisitedIds = new HashSet<>(currentLevelIds);
        Map<UUID, KnowledgeTopic> loadedTopics = new HashMap<>();
        loadedTopics.put(centerTopic.getId(), centerTopic);

        for (int d = 1; d <= boundedDepth; d++) {
            if (currentLevelIds.isEmpty() || allVisitedIds.size() >= maxAllowedNodes) {
                break;
            }

            List<KnowledgeTopicRelationship> stepRels = relationshipRepository.findRelationshipsByTopicIds(currentLevelIds);
            Set<UUID> nextLevelIds = new HashSet<>();

            for (KnowledgeTopicRelationship r : stepRels) {
                UUID sId = r.getSourceTopic().getId();
                UUID tId = r.getTargetTopic().getId();

                if (allVisitedIds.size() < maxAllowedNodes && allVisitedIds.add(sId)) {
                    nextLevelIds.add(sId);
                    loadedTopics.put(sId, r.getSourceTopic());
                }
                if (allVisitedIds.size() < maxAllowedNodes && allVisitedIds.add(tId)) {
                    nextLevelIds.add(tId);
                    loadedTopics.put(tId, r.getTargetTopic());
                }
            }
            currentLevelIds = nextLevelIds;
        }

        // Fetch remaining topic entities if any were not in loadedTopics map
        Set<UUID> missingIds = new HashSet<>(allVisitedIds);
        missingIds.removeAll(loadedTopics.keySet());
        if (!missingIds.isEmpty()) {
            topicRepository.findByIdIn(missingIds).forEach(t -> loadedTopics.put(t.getId(), t));
        }

        // Build edges strictly between all collected nodes
        List<KnowledgeTopicRelationship> candidateRels = relationshipRepository.findRelationshipsByTopicIds(allVisitedIds);
        List<GraphEdgeDto> edges = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();

        for (KnowledgeTopicRelationship r : candidateRels) {
            UUID sId = r.getSourceTopic().getId();
            UUID tId = r.getTargetTopic().getId();
            if (allVisitedIds.contains(sId) && allVisitedIds.contains(tId)) {
                String edgeKey = sId + "->" + tId + ":" + r.getRelationshipType();
                if (seenEdges.add(edgeKey)) {
                    edges.add(new GraphEdgeDto(
                            r.getId(),
                            sId,
                            tId,
                            r.getRelationshipType(),
                            r.getDescription(),
                            r.isBidirectional()
                    ));
                }
            }
        }

        List<GraphNodeDto> nodes = loadedTopics.values().stream()
                .map(t -> toGraphNode(t, t.getId().equals(centerTopic.getId())))
                .toList();

        return new KnowledgeGraphResponse(
                centerTopic.getId(),
                centerTopic.getName(),
                boundedDepth,
                nodes.size(),
                edges.size(),
                nodes,
                edges
        );
    }

    private GraphNodeDto toGraphNode(KnowledgeTopic topic, boolean isCenter) {
        int prereqCount = relationshipRepository
                .findByTargetTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF).size();
        int unlockedCount = relationshipRepository
                .findBySourceTopicIdAndRelationshipType(topic.getId(), RelationshipType.PREREQUISITE_OF).size();

        return new GraphNodeDto(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getSubject().getId(),
                topic.getSubject().getName(),
                topic.getTopicType(),
                topic.getDifficultyLevel(),
                topic.getImportanceScore() != null ? topic.getImportanceScore().doubleValue() : 1.0,
                prereqCount,
                unlockedCount,
                isCenter
        );
    }
}

