package com.csscholar.backend.learning.service;

import com.csscholar.backend.learning.dto.*;
import com.csscholar.backend.learning.entity.Flashcard;
import com.csscholar.backend.learning.entity.LearningSession;
import com.csscholar.backend.learning.entity.QuizAttempt;
import com.csscholar.backend.learning.enums.AttemptStatus;
import com.csscholar.backend.learning.enums.ReviewState;
import com.csscholar.backend.learning.enums.SessionType;
import com.csscholar.backend.learning.repository.*;
import com.csscholar.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningAnalyticsService {

    private final QuizAttemptRepository attemptRepository;
    private final FlashcardDeckRepository deckRepository;
    private final FlashcardRepository cardRepository;
    private final FlashcardReviewStateRepository reviewStateRepository;
    private final FlashcardReviewHistoryRepository reviewHistoryRepository;
    private final LearningSessionRepository sessionRepository;

    public LearningAnalyticsService(QuizAttemptRepository attemptRepository,
                                    FlashcardDeckRepository deckRepository,
                                    FlashcardRepository cardRepository,
                                    FlashcardReviewStateRepository reviewStateRepository,
                                    FlashcardReviewHistoryRepository reviewHistoryRepository,
                                    LearningSessionRepository sessionRepository) {
        this.attemptRepository = attemptRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.reviewStateRepository = reviewStateRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Get aggregate overview metrics for user dashboard.
     */
    @Transactional(readOnly = true)
    public LearningAnalyticsOverviewDto getOverview(UUID userId) {
        Instant now = Instant.now();

        long totalAttempts = attemptRepository.countByUserIdAndStatus(userId, AttemptStatus.SUBMITTED);
        Double avgScore = attemptRepository.getAverageScoreByUserIdAndStatus(userId, AttemptStatus.SUBMITTED);
        double averageScore = avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0;

        long totalDecks = deckRepository.countByUserId(userId);
        long totalCards = cardRepository.countByUserId(userId);
        long cardsDueToday = reviewStateRepository.countByUserIdAndNextDueAtLessThanEqual(userId, now);
        long cardsReviewed = reviewHistoryRepository.countByUserId(userId);

        long newCount = reviewStateRepository.countByUserIdAndState(userId, ReviewState.NEW);
        long learningCount = reviewStateRepository.countByUserIdAndState(userId, ReviewState.LEARNING);
        long reviewCount = reviewStateRepository.countByUserIdAndState(userId, ReviewState.REVIEW);
        long graduatedCount = reviewStateRepository.countByUserIdAndState(userId, ReviewState.GRADUATED);

        List<QuizAttemptResponseDto> recentAttempts = attemptRepository
                .findTop5ByUserIdAndStatusOrderBySubmittedAtDesc(userId, AttemptStatus.SUBMITTED)
                .stream()
                .map(a -> new QuizAttemptResponseDto(
                        a.getId(),
                        a.getQuiz().getId(),
                        a.getQuiz().getTitle(),
                        a.getStatus(),
                        a.getScore(),
                        a.getTotalQuestions(),
                        a.getCorrectAnswersCount(),
                        a.getTimeSpentSeconds(),
                        a.getStartedAt(),
                        a.getSubmittedAt(),
                        Collections.emptyList()
                ))
                .collect(Collectors.toList());

        List<LearningSessionDto> recentSessions = sessionRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(s -> new LearningSessionDto(
                        s.getId(),
                        s.getSessionType(),
                        s.getReferenceId(),
                        s.getDurationSeconds(),
                        s.getItemsCompleted(),
                        s.getScorePercentage(),
                        s.getCreatedAt()
                ))
                .collect(Collectors.toList());

        LearningAnalyticsOverviewDto dto = new LearningAnalyticsOverviewDto();
        dto.setTotalQuizAttempts(totalAttempts);
        dto.setAverageQuizScore(averageScore);
        dto.setTotalDecks(totalDecks);
        dto.setTotalCards(totalCards);
        dto.setCardsDueToday(cardsDueToday);
        dto.setCardsReviewedCount(cardsReviewed);
        dto.setNewCardsCount(newCount);
        dto.setLearningCardsCount(learningCount);
        dto.setReviewCardsCount(reviewCount);
        dto.setGraduatedCardsCount(graduatedCount);
        dto.setRecentQuizAttempts(recentAttempts);
        dto.setRecentSessions(recentSessions);

        return dto;
    }

    /**
     * Compute topic-level accuracy and mastery across quizzes and flashcards.
     */
    @Transactional(readOnly = true)
    public List<TopicPerformanceDto> getTopicPerformance(UUID userId) {
        Map<String, List<Double>> topicScores = new HashMap<>();
        List<QuizAttempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(userId);

        for (QuizAttempt attempt : attempts) {
            if (attempt.getStatus() == AttemptStatus.SUBMITTED && attempt.getScore() != null) {
                String topicName = attempt.getQuiz().getTitle();
                if (attempt.getQuiz().getTopic() != null) {
                    topicName = attempt.getQuiz().getTopic().getName();
                }
                topicScores.computeIfAbsent(topicName, k -> new ArrayList<>()).add(attempt.getScore());
            }
        }

        // Count cards by topic
        Map<String, Integer> topicTotalCards = new HashMap<>();
        Map<String, Integer> topicMasteredCards = new HashMap<>();
        List<Flashcard> cards = cardRepository.findAll().stream()
                .filter(c -> c.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        for (Flashcard card : cards) {
            String topicName = card.getTopicName() != null ? card.getTopicName() : card.getDeck().getTitle();
            topicTotalCards.merge(topicName, 1, Integer::sum);
            if (card.getReviewState() != null && card.getReviewState().getState() == ReviewState.GRADUATED) {
                topicMasteredCards.merge(topicName, 1, Integer::sum);
            }
        }

        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(topicScores.keySet());
        allTopics.addAll(topicTotalCards.keySet());

        List<TopicPerformanceDto> results = new ArrayList<>();
        for (String topic : allTopics) {
            List<Double> scores = topicScores.getOrDefault(topic, Collections.emptyList());
            double avg = scores.isEmpty() ? 0.0 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            avg = Math.round(avg * 10.0) / 10.0;

            int totalCards = topicTotalCards.getOrDefault(topic, 0);
            int masteredCards = topicMasteredCards.getOrDefault(topic, 0);

            results.add(new TopicPerformanceDto(
                    topic,
                    scores.size(),
                    avg,
                    totalCards,
                    masteredCards
            ));
        }

        results.sort((a, b) -> Integer.compare(b.getQuizzesTaken(), a.getQuizzesTaken()));
        return results;
    }

    /**
     * Compute daily study activity over the last N days.
     */
    @Transactional(readOnly = true)
    public List<DailyActivityDto> getActivityHistory(UUID userId, int days) {
        if (days <= 0 || days > 90) days = 14;

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<LearningSession> sessions = sessionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, since);

        Map<LocalDate, int[]> dailyMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), new int[]{0, 0, 0}); // [quizCount, reviewCount, durationSeconds]
        }

        for (LearningSession s : sessions) {
            LocalDate date = s.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            int[] metrics = dailyMap.get(date);
            if (metrics != null) {
                if (s.getSessionType() == SessionType.QUIZ_ATTEMPT) {
                    metrics[0] += 1;
                } else if (s.getSessionType() == SessionType.FLASHCARD_REVIEW) {
                    metrics[1] += s.getItemsCompleted();
                }
                metrics[2] += s.getDurationSeconds();
            }
        }

        List<DailyActivityDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : dailyMap.entrySet()) {
            int minutes = (int) Math.round(entry.getValue()[2] / 60.0);
            result.add(new DailyActivityDto(
                    entry.getKey().toString(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    minutes
            ));
        }

        return result;
    }
}
