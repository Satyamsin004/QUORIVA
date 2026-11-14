package com.csscholar.backend.library.repository;

import com.csscholar.backend.library.entity.LibraryModerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibraryModerationLogRepository extends JpaRepository<LibraryModerationLog, UUID> {

    List<LibraryModerationLog> findByResourceIdOrderByCreatedAtDesc(UUID resourceId);

    boolean existsByResourceId(UUID resourceId);

    Page<LibraryModerationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

