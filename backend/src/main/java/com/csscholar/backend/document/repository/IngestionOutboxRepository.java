package com.csscholar.backend.document.repository;

import com.csscholar.backend.document.entity.IngestionOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionOutboxRepository extends JpaRepository<IngestionOutbox, UUID> {

    @Query("SELECT o FROM IngestionOutbox o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<IngestionOutbox> findPendingEvents(@Param("status") String status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE IngestionOutbox o SET o.status = 'DISPATCHING' WHERE o.id = :id AND o.status = 'PENDING'")
    int claimOutboxForDispatch(@Param("id") UUID id);

    List<IngestionOutbox> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}

