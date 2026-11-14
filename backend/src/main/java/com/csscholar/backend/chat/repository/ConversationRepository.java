package com.csscholar.backend.chat.repository;

import com.csscholar.backend.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    Page<Conversation> findByUserIdAndWorkspaceIdOrderByLastMessageAtDesc(UUID userId, UUID workspaceId, Pageable pageable);

    List<Conversation> findByUserIdAndWorkspaceIdOrderByLastMessageAtDesc(UUID userId, UUID workspaceId);

    void deleteByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}

