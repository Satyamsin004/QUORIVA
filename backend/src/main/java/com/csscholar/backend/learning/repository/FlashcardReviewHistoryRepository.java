package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.FlashcardReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashcardReviewHistoryRepository extends JpaRepository<FlashcardReviewHistory, UUID> {

    List<FlashcardReviewHistory> findByUserIdOrderByReviewedAtDesc(UUID userId);

    List<FlashcardReviewHistory> findByCardIdOrderByReviewedAtDesc(UUID cardId);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(h) FROM FlashcardReviewHistory h WHERE h.user.id = :userId AND h.reviewedAt >= :since")
    long countByUserIdAndReviewedAtAfter(@Param("userId") UUID userId, @Param("since") Instant since);
}

