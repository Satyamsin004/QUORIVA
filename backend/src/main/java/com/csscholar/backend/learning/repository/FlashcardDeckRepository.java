package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.FlashcardDeck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, UUID> {

    List<FlashcardDeck> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Page<FlashcardDeck> findByUserId(UUID userId, Pageable pageable);

    Page<FlashcardDeck> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId, Pageable pageable);

    Optional<FlashcardDeck> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}

