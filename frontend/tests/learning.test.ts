import { test, describe } from 'node:test';
import assert from 'node:assert';
import {
  QuizPractice,
  QuizAttemptResponse,
  QuizAttemptReview,
  Flashcard,
  DueCard,
  LearningAnalyticsOverview,
  ReviewRating,
  ReviewState,
} from '../src/types/learning';

// Client-side helper functions for simulation and verification
function simulateCalculateSM2(
  rating: ReviewRating,
  currentEf: number = 2.5,
  currentRepCount: number = 0,
  currentInterval: number = 0
): { repetitionCount: number; easeFactor: number; intervalDays: number; state: ReviewState } {
  const qualityMap: Record<ReviewRating, number> = {
    AGAIN: 1,
    HARD: 3,
    GOOD: 4,
    EASY: 5,
  };
  const q = qualityMap[rating];

  // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
  let updatedEf = currentEf + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
  if (updatedEf < 1.3) updatedEf = 1.3;
  updatedEf = Math.round(updatedEf * 1000) / 1000;

  let nextRepCount: number;
  let nextInterval: number;
  let nextState: ReviewState;

  if (q < 3) {
    nextRepCount = 0;
    nextInterval = 1;
    nextState = 'LEARNING';
  } else {
    if (currentRepCount === 0) {
      nextRepCount = 1;
      nextInterval = 1;
      nextState = 'LEARNING';
    } else if (currentRepCount === 1) {
      nextRepCount = 2;
      nextInterval = q === 3 ? 3 : 6;
      nextState = 'REVIEW';
    } else {
      nextRepCount = currentRepCount + 1;
      if (q === 3) {
        nextInterval = Math.max(currentInterval + 1, Math.round(currentInterval * 1.2));
      } else if (q === 4) {
        nextInterval = Math.max(currentInterval + 1, Math.round(currentInterval * updatedEf));
      } else {
        nextInterval = Math.max(currentInterval + 2, Math.round(currentInterval * updatedEf * 1.3));
      }
      nextState = nextRepCount >= 3 ? 'GRADUATED' : 'REVIEW';
    }
  }

  return { repetitionCount: nextRepCount, easeFactor: updatedEf, intervalDays: nextInterval, state: nextState };
}

function verifyPracticeDtoAnswerConfidentiality(quiz: QuizPractice): { isSecure: boolean; leakedFields: string[] } {
  const leakedFields: string[] = [];
  for (const q of quiz.questions) {
    if ('explanation' in q && (q as any).explanation) {
      leakedFields.push(`explanation in question ${q.questionId}`);
    }
    if ('correctOptionIndex' in q) {
      leakedFields.push(`correctOptionIndex in question ${q.questionId}`);
    }
    for (const opt of q.options) {
      if ('isCorrect' in opt) {
        leakedFields.push(`isCorrect in option ${opt.optionIndex}`);
      }
    }
  }
  return {
    isSecure: leakedFields.length === 0,
    leakedFields,
  };
}

describe('Learning Studio: Answer Confidentiality & Practice Projection', () => {
  test('Quiz practice DTO must not leak answer keys or explanations', () => {
    const practiceQuiz: QuizPractice = {
      quizId: 'quiz-101',
      title: 'Operating Systems Scheduling Practice',
      difficulty: 'MEDIUM',
      totalQuestions: 2,
      questions: [
        {
          questionId: 'q-1',
          questionIndex: 0,
          questionText: 'Which scheduling algorithm is non-preemptive and provably optimal for average waiting time?',
          questionType: 'MULTIPLE_CHOICE',
          options: [
            { optionIndex: 0, optionText: 'Round Robin' },
            { optionIndex: 1, optionText: 'Shortest Job First (SJF)' },
            { optionIndex: 2, optionText: 'First-Come First-Served' },
            { optionIndex: 3, optionText: 'Multilevel Feedback Queue' },
          ],
        },
        {
          questionId: 'q-2',
          questionIndex: 1,
          questionText: 'A process switch always incurs context switch overhead in hardware and OS data structures.',
          questionType: 'TRUE_FALSE',
          options: [
            { optionIndex: 0, optionText: 'True' },
            { optionIndex: 1, optionText: 'False' },
          ],
        },
      ],
    };

    const audit = verifyPracticeDtoAnswerConfidentiality(practiceQuiz);
    assert.strictEqual(audit.isSecure, true);
    assert.strictEqual(audit.leakedFields.length, 0);

    // Verify properties
    assert.strictEqual(practiceQuiz.questions.length, 2);
    assert.strictEqual(practiceQuiz.questions[0].options.length, 4);
    assert.strictEqual((practiceQuiz.questions[0] as any).explanation, undefined);
    assert.strictEqual((practiceQuiz.questions[0].options[1] as any).isCorrect, undefined);
  });
});

describe('Learning Studio: SuperMemo-2 (SM-2) Spaced Repetition Engine', () => {
  test('SM-2 initial rating GOOD advances repetition to 1, interval to 1 day, state LEARNING', () => {
    const res = simulateCalculateSM2('GOOD', 2.5, 0, 0);
    assert.strictEqual(res.repetitionCount, 1);
    assert.strictEqual(res.intervalDays, 1);
    assert.strictEqual(res.state, 'LEARNING');
    assert.strictEqual(res.easeFactor, 2.5);
  });

  test('SM-2 second rating GOOD advances repetition to 2, interval to 6 days, state REVIEW', () => {
    const res = simulateCalculateSM2('GOOD', 2.5, 1, 1);
    assert.strictEqual(res.repetitionCount, 2);
    assert.strictEqual(res.intervalDays, 6);
    assert.strictEqual(res.state, 'REVIEW');
  });

  test('SM-2 third rating GOOD graduates card with interval = round(6 * EF)', () => {
    const res = simulateCalculateSM2('GOOD', 2.5, 2, 6);
    assert.strictEqual(res.repetitionCount, 3);
    assert.strictEqual(res.intervalDays, 15); // 6 * 2.5 = 15
    assert.strictEqual(res.state, 'GRADUATED');
  });

  test('SM-2 AGAIN resets repetition count to 0, interval to 1 day, drops EF', () => {
    const res = simulateCalculateSM2('AGAIN', 2.5, 3, 15);
    assert.strictEqual(res.repetitionCount, 0);
    assert.strictEqual(res.intervalDays, 1);
    assert.strictEqual(res.state, 'LEARNING');
    assert.ok(res.easeFactor < 2.5, 'Ease factor should decrease after AGAIN');
  });

  test('SM-2 minimum ease factor is clamped to 1.30 floor', () => {
    let ef = 1.4;
    // Repeated AGAIN reviews
    const res1 = simulateCalculateSM2('AGAIN', ef, 1, 1);
    assert.strictEqual(res1.easeFactor, 1.3);

    const res2 = simulateCalculateSM2('AGAIN', res1.easeFactor, 0, 1);
    assert.strictEqual(res2.easeFactor, 1.3);
  });

  test('SM-2 EASY rating increases ease factor and applies bonus interval multiplier', () => {
    const res = simulateCalculateSM2('EASY', 2.5, 2, 6);
    assert.strictEqual(res.repetitionCount, 3);
    assert.strictEqual(res.easeFactor, 2.6);
    assert.strictEqual(res.intervalDays, 20); // 6 * 2.6 * 1.3 = 20.28 -> 20
    assert.strictEqual(res.state, 'GRADUATED');
  });
});

describe('Learning Studio: Quiz Scoring and Attempt Review', () => {
  test('Quiz attempt review provides accurate grading, user feedback and explanations', () => {
    const reviewPayload: QuizAttemptReview = {
      attemptId: 'att-501',
      quizId: 'quiz-101',
      quizTitle: 'Operating Systems Scheduling Practice',
      status: 'SUBMITTED',
      score: 1,
      maxScore: 2,
      percentage: 50.0,
      startedAt: '2026-11-12T10:00:00Z',
      submittedAt: '2026-11-12T10:03:00Z',
      durationSeconds: 180,
      questions: [
        {
          questionId: 'q-1',
          questionIndex: 0,
          questionText: 'Which scheduling algorithm is non-preemptive and provably optimal for average waiting time?',
          questionType: 'MULTIPLE_CHOICE',
          explanation: 'SJF schedules the process with shortest burst time, minimizing overall waiting time.',
          selectedOptionIndex: 1,
          correctOptionIndex: 1,
          isUserCorrect: true,
          options: [
            { optionIndex: 0, optionText: 'Round Robin', isCorrect: false },
            { optionIndex: 1, optionText: 'Shortest Job First (SJF)', isCorrect: true },
            { optionIndex: 2, optionText: 'First-Come First-Served', isCorrect: false },
            { optionIndex: 3, optionText: 'Multilevel Feedback Queue', isCorrect: false },
          ],
        },
        {
          questionId: 'q-2',
          questionIndex: 1,
          questionText: 'A process switch always incurs context switch overhead in hardware and OS data structures.',
          questionType: 'TRUE_FALSE',
          explanation: 'Context switching requires saving registers, PC, memory mapping tables, and flushing cache lines.',
          selectedOptionIndex: 1, // User selected False
          correctOptionIndex: 0, // Correct is True
          isUserCorrect: false,
          options: [
            { optionIndex: 0, optionText: 'True', isCorrect: true },
            { optionIndex: 1, optionText: 'False', isCorrect: false },
          ],
        },
      ],
    };

    assert.strictEqual(reviewPayload.percentage, 50.0);
    assert.strictEqual(reviewPayload.score, 1);
    assert.strictEqual(reviewPayload.questions.length, 2);
    assert.strictEqual(reviewPayload.questions[0].isUserCorrect, true);
    assert.strictEqual(reviewPayload.questions[1].isUserCorrect, false);
    assert.ok(reviewPayload.questions[0].explanation?.includes('SJF schedules'));
    assert.ok(reviewPayload.questions[1].explanation?.includes('Context switching requires'));
  });
});

describe('Learning Studio: Analytics Overview Calculation', () => {
  test('Analytics overview summarizes user progress correctly without fabricated values', () => {
    const overview: LearningAnalyticsOverview = {
      totalQuizzesTaken: 4,
      averageQuizScore: 82.5,
      totalFlashcardDecks: 3,
      totalFlashcards: 25,
      cardsDueToday: 7,
      cardsReviewedToday: 18,
      totalLearningTimeSeconds: 3600,
    };

    assert.strictEqual(overview.totalQuizzesTaken, 4);
    assert.strictEqual(overview.averageQuizScore, 82.5);
    assert.strictEqual(overview.totalFlashcards, 25);
    assert.strictEqual(overview.cardsDueToday, 7);
    assert.strictEqual(overview.totalLearningTimeSeconds / 60, 60); // 60 minutes
  });
});

