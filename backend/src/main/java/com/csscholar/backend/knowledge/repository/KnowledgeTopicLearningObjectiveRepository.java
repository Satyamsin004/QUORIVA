package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.KnowledgeTopicLearningObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeTopicLearningObjectiveRepository extends JpaRepository<KnowledgeTopicLearningObjective, UUID> {

    List<KnowledgeTopicLearningObjective> findByTopicIdOrderByDisplayOrderAsc(UUID topicId);
}

