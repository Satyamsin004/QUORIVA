package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.KnowledgeTopicTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeTopicTagRepository extends JpaRepository<KnowledgeTopicTag, UUID> {

    List<KnowledgeTopicTag> findByTopicId(UUID topicId);
}

