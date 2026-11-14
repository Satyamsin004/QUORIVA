package com.csscholar.backend.chat.repository;

import com.csscholar.backend.chat.entity.MessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageSourceRepository extends JpaRepository<MessageSource, UUID> {

    List<MessageSource> findByMessageIdOrderBySourceRankAsc(UUID messageId);
}

