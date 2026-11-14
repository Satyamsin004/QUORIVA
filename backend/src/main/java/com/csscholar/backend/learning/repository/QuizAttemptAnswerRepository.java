package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.QuizAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, UUID> {

    List<QuizAttemptAnswer> findByAttemptId(UUID attemptId);

    Optional<QuizAttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    long countByAttemptIdAndIsCorrectTrue(UUID attemptId);
}

