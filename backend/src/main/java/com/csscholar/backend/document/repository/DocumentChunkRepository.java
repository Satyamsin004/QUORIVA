package com.csscholar.backend.document.repository;

import com.csscholar.backend.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdAndProcessingVersionOrderByChunkIndexAsc(UUID documentId, int processingVersion);

    List<DocumentChunk> findByDocumentIdAndPageNumberBetweenOrderByPageNumberAscChunkIndexAsc(UUID documentId, Integer startPage, Integer endPage);

    long countByDocumentIdAndProcessingVersion(UUID documentId, int processingVersion);
}

