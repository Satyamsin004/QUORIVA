package com.csscholar.backend.learning.repository;

import com.csscholar.backend.learning.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdOrderByQuestionIndexAsc(UUID quizId);

    long countByQuizId(UUID quizId);
}

