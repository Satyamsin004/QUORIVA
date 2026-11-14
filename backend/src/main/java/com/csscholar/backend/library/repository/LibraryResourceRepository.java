package com.csscholar.backend.library.repository;

import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LibraryResourceRepository extends JpaRepository<LibraryResource, UUID>, JpaSpecificationExecutor<LibraryResource> {

    Optional<LibraryResource> findByStorageKey(String storageKey);

    Page<LibraryResource> findByContributorIdOrderByCreatedAtDesc(UUID contributorId, Pageable pageable);

    Page<LibraryResource> findByContributorIdAndStatusNotOrderByCreatedAtDesc(UUID contributorId, LibraryResourceStatus status, Pageable pageable);

    Page<LibraryResource> findByStatusOrderByCreatedAtAsc(LibraryResourceStatus status, Pageable pageable);

    long countByStatus(LibraryResourceStatus status);

    @Modifying
    @Query("UPDATE LibraryResource r SET r.downloadsCount = r.downloadsCount + 1 WHERE r.id = :id")
    void incrementDownloadCount(@Param("id") UUID id);
}

