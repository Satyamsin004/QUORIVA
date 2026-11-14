package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeTopicRepository extends JpaRepository<KnowledgeTopic, UUID>, JpaSpecificationExecutor<KnowledgeTopic> {

    Optional<KnowledgeTopic> findBySlug(String slug);

    List<KnowledgeTopic> findBySubjectIdOrderByImportanceScoreDesc(UUID subjectId);

    List<KnowledgeTopic> findByParentTopicId(UUID parentTopicId);

    List<KnowledgeTopic> findByIdIn(Collection<UUID> ids);

    @Query("SELECT t FROM KnowledgeTopic t WHERE " +
           "(:subjectId IS NULL OR t.subject.id = :subjectId) AND " +
           "(t.status = 'PUBLISHED') ORDER BY t.importanceScore DESC")
    List<KnowledgeTopic> findPublishedBySubjectId(@Param("subjectId") UUID subjectId, Pageable pageable);
}

