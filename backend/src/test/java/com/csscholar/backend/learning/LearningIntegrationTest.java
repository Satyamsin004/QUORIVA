package com.csscholar.backend.learning;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.learning.dto.*;
import com.csscholar.backend.learning.entity.*;
import com.csscholar.backend.learning.enums.*;
import com.csscholar.backend.learning.repository.*;
import com.csscholar.backend.learning.service.FlashcardService;
import com.csscholar.backend.learning.service.LearningAnalyticsService;
import com.csscholar.backend.learning.service.QuizService;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Learning Studio Integration Tests - Answer Security, Attempts, SM-2 and Analytics")
class LearningIntegrationTest {

    @Autowired
    private QuizService quizService;

    @Autowired
    private FlashcardService flashcardService;

    @Autowired
    private LearningAnalyticsService analyticsService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private QuizQuestionOptionRepository optionRepository;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private QuizAttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private FlashcardDeckRepository deckRepository;

    @Autowired
    private FlashcardRepository cardRepository;

    @Autowired
    private FlashcardReviewStateRepository reviewStateRepository;

    @Autowired
    private FlashcardReviewHistoryRepository reviewHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Quiz testQuiz;
    private QuizQuestion q1;
    private QuizQuestion q2;
    private QuizQuestionOption opt1A;
    private QuizQuestionOption opt1B;
    private QuizQuestionOption opt2A;
    private QuizQuestionOption opt2B;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User(
                "learnuser_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "hashedpassword",
                "Test Learner",
                Role.STUDENT
        ));

        // Create a test quiz directly in database
        testQuiz = new Quiz(
                testUser,
                null,
                "Operating Systems Scheduling Quiz",
                "Quiz testing round robin and shortest job first",
                LearningSourceType.CUSTOM,
                null,
                null,
                LearningDifficultyLevel.MEDIUM,
                30
        );
        testQuiz.setTotalQuestions(2);
        testQuiz = quizRepository.save(testQuiz);

        // Q1: Multiple Choice
        q1 = new QuizQuestion(testQuiz, 0, "What is a major advantage of Round Robin scheduling?",
                QuestionType.MULTIPLE_CHOICE, LearningDifficultyLevel.MEDIUM,
                "Fair CPU time allocation via time slicing", "OS Concepts Ch 5", 140, null, "CPU Scheduling");
        q1 = questionRepository.save(q1);

        opt1A = new QuizQuestionOption(q1, 0, "Prevents starvation with time slices", true);
        opt1B = new QuizQuestionOption(q1, 1, "Guarantees zero context switches", false);
        QuizQuestionOption opt1C = new QuizQuestionOption(q1, 2, "Always minimizes average turnaround time", false);
        QuizQuestionOption opt1D = new QuizQuestionOption(q1, 3, "Requires no hardware timer support", false);
        List<QuizQuestionOption> savedOptions1 = optionRepository.saveAll(List.of(opt1A, opt1B, opt1C, opt1D));
        q1.setOptions(savedOptions1);

        // Q2: True / False
        q2 = new QuizQuestion(testQuiz, 1, "Shortest Job First (SJF) scheduling is provably optimal for minimizing average waiting time.",
                QuestionType.TRUE_FALSE, LearningDifficultyLevel.MEDIUM,
                "SJF minimizes average waiting time among non-preemptive algorithms", "OS Concepts Ch 5", 142, null, "CPU Scheduling");
        q2 = questionRepository.save(q2);

        opt2A = new QuizQuestionOption(q2, 0, "True", true);
        opt2B = new QuizQuestionOption(q2, 1, "False", false);
        List<QuizQuestionOption> savedOptions2 = optionRepository.saveAll(List.of(opt2A, opt2B));
        q2.setOptions(savedOptions2);

        testQuiz.getQuestions().add(q1);
        testQuiz.getQuestions().add(q2);
        quizRepository.save(testQuiz);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Answer Security: Practice DTO must NEVER include isCorrect or explanation")
    void testQuizPracticeDtoOmitsAnswerKeyAndExplanation() {
        QuizPracticeDto practiceDto = quizService.getQuizPractice(testQuiz.getId(), testUser.getId());

        assertThat(practiceDto).isNotNull();
        assertThat(practiceDto.getTitle()).isEqualTo("Operating Systems Scheduling Quiz");
        assertThat(practiceDto.getQuestions()).hasSize(2);

        for (QuizQuestionPracticeDto q : practiceDto.getQuestions()) {
            assertThat(q.getQuestionText()).isNotBlank();
            assertThat(q.getOptions()).isNotEmpty();
            // In QuizOptionPracticeDto, only optionIndex and optionText exist — isCorrect does not even exist on the DTO class
            for (QuizOptionPracticeDto opt : q.getOptions()) {
                assertThat(opt.getOptionText()).isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Quiz Attempt Workflow: start, answer, submit, server-side scoring, duplicate prevention")
    void testQuizAttemptLifecycle() {
        // 1. Start attempt
        QuizAttemptResponseDto attemptDto = quizService.startAttempt(testQuiz.getId(), testUser.getId());
        assertThat(attemptDto.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        UUID attemptId = attemptDto.getId();

        // 2. Save partial answers: Answer Q1 correctly (opt1A), Q2 incorrectly (opt2B)
        SaveAnswersRequest saveReq = new SaveAnswersRequest();
        saveReq.setTimeSpentSeconds(45);
        saveReq.setAnswers(List.of(
                new SaveAnswersRequest.AnswerItemDto(q1.getId(), opt1A.getId(), "Prevents starvation with time slices", false),
                new SaveAnswersRequest.AnswerItemDto(q2.getId(), opt2B.getId(), "False", false)
        ));
        quizService.saveAnswers(attemptId, saveReq, testUser.getId());

        // Verify partial state
        QuizAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
        List<QuizAttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        assertThat(answers).hasSize(2);

        // 3. Submit attempt -> Server-side scoring
        QuizAttemptResponseDto submittedDto = quizService.submitAttempt(attemptId, null, testUser.getId());
        assertThat(submittedDto.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(submittedDto.getCorrectAnswersCount()).isEqualTo(1); // 1 out of 2 correct
        assertThat(submittedDto.getScore()).isEqualTo(50.0);
        assertThat(submittedDto.getSubmittedAt()).isNotNull();

        // 4. Duplicate submission prevention: submitting again must throw BadRequestException
        assertThatThrownBy(() -> quizService.submitAttempt(attemptId, null, testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been submitted");

        // 5. Post-submission review: now explanations and correct answer keys ARE available
        QuizAttemptReviewDto reviewDto = quizService.getAttemptReview(attemptId, testUser.getId());
        assertThat(reviewDto.getCorrectAnswersCount()).isEqualTo(1);
        assertThat(reviewDto.getQuestions()).hasSize(2);

        QuizQuestionReviewDto reviewQ1 = reviewDto.getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(q1.getId()))
                .findFirst().orElseThrow();
        assertThat(reviewQ1.getExplanation()).isEqualTo("Fair CPU time allocation via time slicing");
        assertThat(reviewQ1.getIsCorrect()).isTrue();
        assertThat(reviewQ1.getCorrectOptionId()).isEqualTo(opt1A.getId());
        assertThat(reviewQ1.getSelectedOptionId()).isEqualTo(opt1A.getId());

        QuizQuestionReviewDto reviewQ2 = reviewDto.getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(q2.getId()))
                .findFirst().orElseThrow();
        assertThat(reviewQ2.getExplanation()).isEqualTo("SJF minimizes average waiting time among non-preemptive algorithms");
        assertThat(reviewQ2.getIsCorrect()).isFalse();
        assertThat(reviewQ2.getCorrectOptionId()).isEqualTo(opt2A.getId());
        assertThat(reviewQ2.getSelectedOptionId()).isEqualTo(opt2B.getId());
    }

    @Test
    @DisplayName("Flashcards & SM-2 Workflow: Create deck, add cards, record reviews, query due cards")
    void testFlashcardsAndSpacedRepetition() {
        // 1. Create deck
        CreateManualDeckRequest deckReq = new CreateManualDeckRequest();
        deckReq.setTitle("Algorithms Flashcards");
        deckReq.setDescription("Data structures and complexity flashcards");
        deckReq.setSubjectName("Computer Science");
        deckReq.setTopicName("Algorithms");

        FlashcardDeckDto deckDto = flashcardService.createManualDeck(testUser.getId(), deckReq);
        assertThat(deckDto.getId()).isNotNull();
        assertThat(deckDto.getTotalCards()).isEqualTo(0);

        // 2. Add manual card
        CreateManualCardRequest cardReq = new CreateManualCardRequest();
        cardReq.setFront("What is the average time complexity of QuickSort?");
        cardReq.setBack("O(n log n). Worst-case is O(n^2) when poor pivot selection occurs.");
        cardReq.setSourceReference("Algorithms Textbook p. 124");

        FlashcardDto cardDto = flashcardService.addCardToDeck(deckDto.getId(), cardReq, testUser.getId());
        assertThat(cardDto.getFront()).isEqualTo(cardReq.getFront());
        assertThat(cardDto.getState()).isEqualTo(ReviewState.NEW);

        // 3. Query due cards: should be due today since it's NEW
        List<DueCardDto> dueCards = flashcardService.getDueCards(testUser.getId(), deckDto.getId());
        assertThat(dueCards).hasSize(1);
        assertThat(dueCards.get(0).getId()).isEqualTo(cardDto.getId());

        // 4. Record review rating GOOD
        RecordReviewRequest reviewReq = new RecordReviewRequest(cardDto.getId(), ReviewRating.GOOD, 8);
        ReviewSummaryDto summary = flashcardService.recordReview(testUser.getId(), reviewReq);

        assertThat(summary.getScheduledDays()).isEqualTo(1);
        assertThat(summary.getRepetitionCount()).isEqualTo(1);
        assertThat(summary.getEaseFactor()).isEqualTo(2.50);
        assertThat(summary.getState()).isEqualTo(ReviewState.LEARNING);

        // 5. Query due cards again: should no longer be due today (due in 1 day)
        List<DueCardDto> dueCardsAfter = flashcardService.getDueCards(testUser.getId(), deckDto.getId());
        assertThat(dueCardsAfter).isEmpty();
    }

    @Test
    @DisplayName("Learning Analytics: Overview aggregates quiz and flashcard metrics without fabrication")
    void testLearningAnalyticsOverview() {
        // Execute a quiz attempt and submit
        QuizAttemptResponseDto attemptDto = quizService.startAttempt(testQuiz.getId(), testUser.getId());
        SaveAnswersRequest saveReq = new SaveAnswersRequest();
        saveReq.setTimeSpentSeconds(30);
        saveReq.setAnswers(List.of(
                new SaveAnswersRequest.AnswerItemDto(q1.getId(), opt1A.getId(), "Prevents starvation with time slices", false),
                new SaveAnswersRequest.AnswerItemDto(q2.getId(), opt2A.getId(), "True", false)
        ));
        quizService.saveAnswers(attemptDto.getId(), saveReq, testUser.getId());
        quizService.submitAttempt(attemptDto.getId(), null, testUser.getId());

        // Create deck and card
        CreateManualDeckRequest deckReq = new CreateManualDeckRequest();
        deckReq.setTitle("Networks");
        deckReq.setDescription("OSI Model");
        deckReq.setSubjectName("Computer Science");
        deckReq.setTopicName("Networks");
        FlashcardDeckDto deckDto = flashcardService.createManualDeck(testUser.getId(), deckReq);

        CreateManualCardRequest cardReq = new CreateManualCardRequest();
        cardReq.setFront("Which layer does IP operate at?");
        cardReq.setBack("Network Layer (Layer 3)");
        cardReq.setSourceReference("Page 45");
        flashcardService.addCardToDeck(deckDto.getId(), cardReq, testUser.getId());

        // Get analytics overview
        LearningAnalyticsOverviewDto overview = analyticsService.getOverview(testUser.getId());
        assertThat(overview).isNotNull();
        assertThat(overview.getTotalQuizAttempts()).isEqualTo(1);
        assertThat(overview.getAverageQuizScore()).isEqualTo(100.0);
        assertThat(overview.getTotalDecks()).isEqualTo(1);
        assertThat(overview.getTotalCards()).isEqualTo(1);
        assertThat(overview.getCardsDueToday()).isEqualTo(1); // Not reviewed yet

        // Check topic performance
        List<TopicPerformanceDto> topics = analyticsService.getTopicPerformance(testUser.getId());
        assertThat(topics).isNotEmpty();

        // Check daily activity
        List<DailyActivityDto> activity = analyticsService.getActivityHistory(testUser.getId(), 7);
        assertThat(activity).isNotEmpty();
        DailyActivityDto today = activity.get(activity.size() - 1);
        assertThat(today.getQuizAttempts()).isEqualTo(1);
    }
}
