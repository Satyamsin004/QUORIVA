package com.csscholar.backend.library.repository;

import com.csscholar.backend.library.entity.LibraryResourceReport;
import com.csscholar.backend.library.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibraryResourceReportRepository extends JpaRepository<LibraryResourceReport, UUID> {

    List<LibraryResourceReport> findByResourceIdOrderByCreatedAtDesc(UUID resourceId);

    Page<LibraryResourceReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<LibraryResourceReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);
}

