package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.KnowledgeTopicRelationship;
import com.csscholar.backend.knowledge.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeTopicRelationshipRepository extends JpaRepository<KnowledgeTopicRelationship, UUID> {

    List<KnowledgeTopicRelationship> findByTargetTopicIdAndRelationshipType(UUID targetTopicId, RelationshipType type);

    List<KnowledgeTopicRelationship> findBySourceTopicIdAndRelationshipType(UUID sourceTopicId, RelationshipType type);

    Optional<KnowledgeTopicRelationship> findBySourceTopicIdAndTargetTopicIdAndRelationshipType(
            UUID sourceTopicId, UUID targetTopicId, RelationshipType type);

    @Query("SELECT r FROM KnowledgeTopicRelationship r WHERE r.sourceTopic.id = :topicId OR r.targetTopic.id = :topicId")
    List<KnowledgeTopicRelationship> findConnectedRelationships(@Param("topicId") UUID topicId);

    @Query("SELECT r FROM KnowledgeTopicRelationship r WHERE r.sourceTopic.id IN :topicIds OR r.targetTopic.id IN :topicIds")
    List<KnowledgeTopicRelationship> findRelationshipsByTopicIds(@Param("topicIds") Collection<UUID> topicIds);

    @Query("SELECT r FROM KnowledgeTopicRelationship r WHERE " +
           "(r.sourceTopic.id = :topicId OR r.targetTopic.id = :topicId) AND " +
           "r.relationshipType = :type")
    List<KnowledgeTopicRelationship> findRelatedRelationships(@Param("topicId") UUID topicId, @Param("type") RelationshipType type);

    @Query("SELECT r FROM KnowledgeTopicRelationship r JOIN FETCH r.sourceTopic JOIN FETCH r.targetTopic")
    List<KnowledgeTopicRelationship> findAllWithTopics();
}

