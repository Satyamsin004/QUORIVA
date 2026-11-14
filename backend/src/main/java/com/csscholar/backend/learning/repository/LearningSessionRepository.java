package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, UUID> {

    List<LearningSession> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LearningSession> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID userId, Instant since);

    long countByUserId(UUID userId);
}

