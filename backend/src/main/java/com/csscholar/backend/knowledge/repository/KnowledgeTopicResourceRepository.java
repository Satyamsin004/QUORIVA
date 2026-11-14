package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.KnowledgeTopicResource;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeTopicResourceRepository extends JpaRepository<KnowledgeTopicResource, UUID> {

    @Query("SELECT r FROM KnowledgeTopicResource r JOIN FETCH r.resource WHERE r.topic.id = :topicId AND r.resource.status = :status")
    List<KnowledgeTopicResource> findByTopicIdAndResourceStatus(@Param("topicId") UUID topicId, @Param("status") LibraryResourceStatus status);
}

