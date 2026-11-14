package com.csscholar.backend.document.repository;

import com.csscholar.backend.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    @Query("SELECT d FROM Document d JOIN FETCH d.owner JOIN FETCH d.workspace WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Document> findActiveByIdWithDetails(@Param("id") UUID id);

    List<Document> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    @Query("SELECT d FROM Document d JOIN FETCH d.owner JOIN FETCH d.workspace WHERE d.id = :id")
    Optional<Document> findByIdWithDetails(@Param("id") UUID id);

    Optional<Document> findByStorageKey(String storageKey);

    Optional<Document> findByStorageKeyAndDeletedAtIsNull(String storageKey);

    boolean existsByStorageKey(String storageKey);
}

