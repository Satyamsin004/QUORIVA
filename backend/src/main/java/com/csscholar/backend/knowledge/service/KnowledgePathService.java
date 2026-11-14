package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.knowledge.dto.LearningPathResponse;
import com.csscholar.backend.knowledge.dto.LearningPathStepResponse;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicRelationship;
import com.csscholar.backend.knowledge.enums.DifficultyLevel;
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
public class KnowledgePathService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePathService.class);

    private final KnowledgeTopicService topicService;
    private final KnowledgeTopicRepository topicRepository;
    private final KnowledgeTopicRelationshipRepository relationshipRepository;

    public KnowledgePathService(KnowledgeTopicService topicService,
                                KnowledgeTopicRepository topicRepository,
                                KnowledgeTopicRelationshipRepository relationshipRepository) {
        this.topicService = topicService;
        this.topicRepository = topicRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public LearningPathResponse generateLearningPath(String idOrSlug) {
        KnowledgeTopic targetTopic = topicService.resolveTopic(idOrSlug);

        // 1. Collect all prerequisite ancestors via BFS/DFS with cycle detection
        Set<UUID> subgraphNodeIds = new HashSet<>();
        subgraphNodeIds.add(targetTopic.getId());

        Queue<UUID> queue = new LinkedList<>();
        queue.add(targetTopic.getId());

        Set<UUID> visited = new HashSet<>();
        visited.add(targetTopic.getId());

        int maxPrereqNodes = 50;

        while (!queue.isEmpty() && subgraphNodeIds.size() < maxPrereqNodes) {
            UUID currentId = queue.poll();

            List<KnowledgeTopicRelationship> incomingRels =
                    relationshipRepository.findByTargetTopicIdAndRelationshipType(currentId, RelationshipType.PREREQUISITE_OF);

            for (KnowledgeTopicRelationship r : incomingRels) {
                UUID prereqId = r.getSourceTopic().getId();
                subgraphNodeIds.add(prereqId);

                if (visited.add(prereqId)) {
                    queue.add(prereqId);
                } else {
                    log.debug("Prerequisite cycle or cross-edge detected at topic: {}", prereqId);
                }
            }
        }

        // Fetch topic entities for all nodes in subgraph
        Map<UUID, KnowledgeTopic> topicMap = new HashMap<>();
        topicRepository.findByIdIn(subgraphNodeIds).forEach(t -> topicMap.put(t.getId(), t));

        // 2. Build DAG adjacency: source -> targets (where source PREREQUISITE_OF target)
        Map<UUID, List<UUID>> adj = new HashMap<>();
        Map<UUID, Integer> inDegree = new HashMap<>();

        for (UUID id : subgraphNodeIds) {
            adj.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }

        List<KnowledgeTopicRelationship> relationships = relationshipRepository.findRelationshipsByTopicIds(subgraphNodeIds);
        for (KnowledgeTopicRelationship r : relationships) {
            if (r.getRelationshipType() == RelationshipType.PREREQUISITE_OF) {
                UUID s = r.getSourceTopic().getId();
                UUID t = r.getTargetTopic().getId();

                if (subgraphNodeIds.contains(s) && subgraphNodeIds.contains(t) && !s.equals(t)) {
                    adj.get(s).add(t);
                    inDegree.put(t, inDegree.get(t) + 1);
                }
            }
        }

        // 3. Topological Sort (Kahn's Algorithm with deterministic priority queue)
        // Priority: Difficulty level (BEGINNER first) -> importance score DESC -> name ASC
        Comparator<KnowledgeTopic> priorityComparator = (a, b) -> {
            int diffCompare = Integer.compare(difficultyOrdinal(a.getDifficultyLevel()), difficultyOrdinal(b.getDifficultyLevel()));
            if (diffCompare != 0) return diffCompare;

            double scoreA = a.getImportanceScore() != null ? a.getImportanceScore().doubleValue() : 1.0;
            double scoreB = b.getImportanceScore() != null ? b.getImportanceScore().doubleValue() : 1.0;
            int scoreCompare = Double.compare(scoreB, scoreA);
            if (scoreCompare != 0) return scoreCompare;

            int nameCompare = a.getName().compareToIgnoreCase(b.getName());
            if (nameCompare != 0) return nameCompare;

            return a.getId().compareTo(b.getId());
        };

        PriorityQueue<KnowledgeTopic> readyQueue = new PriorityQueue<>(priorityComparator);

        for (UUID id : subgraphNodeIds) {
            if (inDegree.get(id) == 0 && topicMap.containsKey(id)) {
                readyQueue.add(topicMap.get(id));
            }
        }

        List<KnowledgeTopic> orderedSequence = new ArrayList<>();
        Set<UUID> addedToSequence = new HashSet<>();

        while (!readyQueue.isEmpty()) {
            KnowledgeTopic current = readyQueue.poll();
            orderedSequence.add(current);
            addedToSequence.add(current.getId());

            for (UUID neighborId : adj.get(current.getId())) {
                inDegree.put(neighborId, inDegree.get(neighborId) - 1);
                if (inDegree.get(neighborId) == 0 && topicMap.containsKey(neighborId)) {
                    readyQueue.add(topicMap.get(neighborId));
                }
            }
        }

        // Cycle verification: If Kahn's algorithm could not dequeue all subgraph nodes, a prerequisite cycle exists
        if (orderedSequence.size() < subgraphNodeIds.size()) {
            throw new KnowledgeGraphIntegrityException(
                    "Prerequisite dependency cycle detected involving topic: '" + targetTopic.getSlug() +
                            "'. Topological ordering requires a cycle-free directed acyclic graph."
            );
        }

        // Ensure targetTopic is positioned at the end of the sequence
        orderedSequence.removeIf(t -> t.getId().equals(targetTopic.getId()));
        orderedSequence.add(targetTopic);

        // 4. Construct response steps
        List<LearningPathStepResponse> steps = new ArrayList<>();
        int totalMinutes = 0;
        int stepNum = 1;

        for (KnowledgeTopic t : orderedSequence) {
            totalMinutes += t.getEstimatedMinutes();
            int directPrereqs = relationshipRepository
                    .findByTargetTopicIdAndRelationshipType(t.getId(), RelationshipType.PREREQUISITE_OF).size();

            steps.add(new LearningPathStepResponse(
                    stepNum++,
                    t.getId(),
                    t.getName(),
                    t.getSlug(),
                    t.getSubject().getName(),
                    t.getDifficultyLevel(),
                    t.getEstimatedMinutes(),
                    t.getSummary(),
                    t.getId().equals(targetTopic.getId()),
                    directPrereqs
            ));
        }

        boolean hasPrereqs = steps.size() > 1;

        return new LearningPathResponse(
                targetTopic.getId(),
                targetTopic.getName(),
                targetTopic.getSlug(),
                steps.size(),
                totalMinutes,
                hasPrereqs,
                steps
        );
    }

    public List<LearningPathResponse> getCuratedLearningPaths() {
        String[] milestoneSlugs = {
                "dynamic-programming",
                "rag-systems",
                "indexing-query-optimization",
                "virtual-memory-paging",
                "design-patterns",
                "instruction-pipelining-hazards",
                "compiler-optimizations-codegen",
                "computational-complexity-np-completeness",
                "network-security-firewalls",
                "ssr-web-performance"
        };

        List<LearningPathResponse> curated = new ArrayList<>();
        for (String slug : milestoneSlugs) {
            try {
                curated.add(generateLearningPath(slug));
            } catch (Exception e) {
                log.warn("Could not generate curated path for slug {}: {}", slug, e.getMessage());
            }
        }
        return curated;
    }

    private int difficultyOrdinal(DifficultyLevel level) {
        if (level == null) return 0;
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
            case EXPERT -> 4;
        };
    }
}

