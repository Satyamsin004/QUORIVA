package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Quiz> findByUserId(UUID userId, Pageable pageable);

    Page<Quiz> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId, Pageable pageable);

    Optional<Quiz> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}

