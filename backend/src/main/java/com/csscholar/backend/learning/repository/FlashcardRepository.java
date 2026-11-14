package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    List<Flashcard> findByDeckIdOrderByCreatedAtAsc(UUID deckId);

    Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByDeckId(UUID deckId);
}

