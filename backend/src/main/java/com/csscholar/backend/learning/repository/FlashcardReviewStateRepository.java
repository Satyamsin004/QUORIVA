package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.FlashcardReviewState;
import com.csscholar.backend.learning.enums.ReviewState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardReviewStateRepository extends JpaRepository<FlashcardReviewState, UUID> {

    Optional<FlashcardReviewState> findByCardId(UUID cardId);

    Optional<FlashcardReviewState> findByCardIdAndUserId(UUID cardId, UUID userId);

    @Query("SELECT s FROM FlashcardReviewState s WHERE s.user.id = :userId AND s.nextDueAt <= :dueDate ORDER BY s.nextDueAt ASC")
    List<FlashcardReviewState> findDueCardsForUser(@Param("userId") UUID userId, @Param("dueDate") Instant dueDate);

    @Query("SELECT s FROM FlashcardReviewState s JOIN s.card c WHERE s.user.id = :userId AND c.deck.id = :deckId AND s.nextDueAt <= :dueDate ORDER BY s.nextDueAt ASC")
    List<FlashcardReviewState> findDueCardsForUserAndDeck(@Param("userId") UUID userId, @Param("deckId") UUID deckId, @Param("dueDate") Instant dueDate);

    long countByUserIdAndNextDueAtLessThanEqual(UUID userId, Instant dueDate);

    long countByUserIdAndState(UUID userId, ReviewState state);
}

