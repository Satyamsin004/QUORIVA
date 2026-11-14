package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicRelationship;
import com.csscholar.backend.knowledge.enums.RelationshipType;
import com.csscholar.backend.knowledge.exception.KnowledgeGraphIntegrityException;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRelationshipRepository;
import com.csscholar.backend.knowledge.repository.KnowledgeTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class KnowledgeGraphIntegrityService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphIntegrityService.class);

    private final KnowledgeTopicRepository topicRepository;
    private final KnowledgeTopicRelationshipRepository relationshipRepository;

    public KnowledgeGraphIntegrityService(KnowledgeTopicRepository topicRepository,
                                         KnowledgeTopicRelationshipRepository relationshipRepository) {
        this.topicRepository = topicRepository;
        this.relationshipRepository = relationshipRepository;
    }

    /**
     * Validates that inserting a prerequisite edge sourceTopicId -> targetTopicId does not violate graph integrity.
     * Convention: sourceTopic is a prerequisite of targetTopic.
     * Rule: targetTopic must not already be able to reach sourceTopic via existing PREREQUISITE_OF edges.
     */
    public void validatePrerequisiteEdge(UUID sourceTopicId, UUID targetTopicId) {
        if (sourceTopicId == null || targetTopicId == null) {
            throw new KnowledgeGraphIntegrityException("Source and target topic IDs must not be null.");
        }

        // 1. Self-reference validation
        if (sourceTopicId.equals(targetTopicId)) {
            throw new KnowledgeGraphIntegrityException(
                    "Self-referential prerequisite is forbidden: Topic " + sourceTopicId + " cannot be a prerequisite of itself."
            );
        }

        // 2. Endpoint existence validation
        if (!topicRepository.existsById(sourceTopicId)) {
            throw new KnowledgeGraphIntegrityException("Source topic does not exist: " + sourceTopicId);
        }
        if (!topicRepository.existsById(targetTopicId)) {
            throw new KnowledgeGraphIntegrityException("Target topic does not exist: " + targetTopicId);
        }

        // 3. Duplicate edge validation
        Optional<KnowledgeTopicRelationship> existing = relationshipRepository
                .findBySourceTopicIdAndTargetTopicIdAndRelationshipType(sourceTopicId, targetTopicId, RelationshipType.PREREQUISITE_OF);
        if (existing.isPresent()) {
            throw new KnowledgeGraphIntegrityException(
                    "Duplicate prerequisite edge: Topic " + sourceTopicId + " is already a prerequisite of " + targetTopicId
            );
        }

        // 4. Cycle prevention: Can targetTopicId already reach sourceTopicId via PREREQUISITE_OF edges?
        Queue<UUID> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();

        queue.add(targetTopicId);
        visited.add(targetTopicId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();

            List<KnowledgeTopicRelationship> outgoing = relationshipRepository
                    .findBySourceTopicIdAndRelationshipType(current, RelationshipType.PREREQUISITE_OF);

            for (KnowledgeTopicRelationship rel : outgoing) {
                UUID reachableNode = rel.getTargetTopic().getId();

                if (reachableNode.equals(sourceTopicId)) {
                    throw new KnowledgeGraphIntegrityException(
                            "Prerequisite cycle detected: Adding edge " + sourceTopicId + " -> " + targetTopicId +
                                    " would create a dependency cycle because " + targetTopicId + " can already reach " + sourceTopicId + "."
                    );
                }

                if (visited.add(reachableNode)) {
                    queue.add(reachableNode);
                }
            }
        }
    }

    /**
     * Comprehensive validation of the entire knowledge graph DAG across all topics.
     */
    public IntegrityReport verifyEntireGraphAcyclic() {
        List<KnowledgeTopic> allTopics = topicRepository.findAll();
        List<KnowledgeTopicRelationship> prereqRelationships = relationshipRepository.findAll()
                .stream()
                .filter(r -> r.getRelationshipType() == RelationshipType.PREREQUISITE_OF)
                .toList();

        Map<UUID, List<UUID>> adj = new HashMap<>();
        Map<UUID, Integer> inDegree = new HashMap<>();

        for (KnowledgeTopic t : allTopics) {
            adj.put(t.getId(), new ArrayList<>());
            inDegree.put(t.getId(), 0);
        }

        for (KnowledgeTopicRelationship r : prereqRelationships) {
            UUID s = r.getSourceTopic().getId();
            UUID t = r.getTargetTopic().getId();

            if (adj.containsKey(s) && adj.containsKey(t)) {
                adj.get(s).add(t);
                inDegree.put(t, inDegree.get(t) + 1);
            }
        }

        Queue<UUID> queue = new LinkedList<>();
        for (UUID id : inDegree.keySet()) {
            if (inDegree.get(id) == 0) {
                queue.add(id);
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            UUID curr = queue.poll();
            processed++;

            for (UUID neighbor : adj.get(curr)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (processed < allTopics.size()) {
            throw new KnowledgeGraphIntegrityException(
                    "Global graph cycle detected: " + (allTopics.size() - processed) +
                            " topics are trapped in a prerequisite cycle and cannot be topologically ordered."
            );
        }

        return new IntegrityReport(allTopics.size(), prereqRelationships.size(), true);
    }

    public record IntegrityReport(int totalTopics, int totalPrerequisiteEdges, boolean isAcyclic) {}
}

