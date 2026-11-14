package com.csscholar.backend.document.repository;

import com.csscholar.backend.document.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    Optional<ProcessingJob> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}

