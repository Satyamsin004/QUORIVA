package com.csscholar.backend.document.repository;

import com.csscholar.backend.document.entity.IngestionOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionOutboxRepository extends JpaRepository<IngestionOutbox, UUID> {

    @Query("SELECT o FROM IngestionOutbox o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<IngestionOutbox> findPendingEvents(@Param("status") String status, Pageable pageable);

    List<IngestionOutbox> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}

