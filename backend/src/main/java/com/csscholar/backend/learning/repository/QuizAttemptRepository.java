package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.QuizAttempt;
import com.csscholar.backend.learning.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(UUID userId);

    List<QuizAttempt> findByQuizIdAndUserIdOrderByStartedAtDesc(UUID quizId, UUID userId);

    Optional<QuizAttempt> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatus(UUID userId, AttemptStatus status);

    @Query("SELECT AVG(a.score) FROM QuizAttempt a WHERE a.user.id = :userId AND a.status = :status AND a.score IS NOT NULL")
    Double getAverageScoreByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") AttemptStatus status);

    List<QuizAttempt> findTop5ByUserIdAndStatusOrderBySubmittedAtDesc(UUID userId, AttemptStatus status);
}

