package com.csscholar.backend.learning.service;

import com.csscholar.backend.learning.entity.FlashcardReviewState;
import com.csscholar.backend.learning.enums.ReviewRating;
import com.csscholar.backend.learning.enums.ReviewState;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Deterministic SuperMemo-2 (SM-2) Spaced Repetition Scheduling Engine.
 */
@Service
public class SpacedRepetitionService {

    public static final double DEFAULT_EASE_FACTOR = 2.5;
    public static final double MIN_EASE_FACTOR = 1.3;

    public static class ReviewCalculationResult {
        private final int repetitionCount;
        private final double easeFactor;
        private final int intervalDays;
        private final Instant nextDueAt;
        private final ReviewState newState;

        public ReviewCalculationResult(int repetitionCount, double easeFactor, int intervalDays,
                                       Instant nextDueAt, ReviewState newState) {
            this.repetitionCount = repetitionCount;
            this.easeFactor = easeFactor;
            this.intervalDays = intervalDays;
            this.nextDueAt = nextDueAt;
            this.newState = newState;
        }

        public int getRepetitionCount() { return repetitionCount; }
        public double getEaseFactor() { return easeFactor; }
        public int getIntervalDays() { return intervalDays; }
        public Instant getNextDueAt() { return nextDueAt; }
        public ReviewState getNewState() { return newState; }
    }

    /**
     * Compute the next SM-2 interval, ease factor, repetition count, and due date.
     */
    public ReviewCalculationResult calculate(FlashcardReviewState current, ReviewRating rating, Instant now) {
        if (now == null) {
            now = Instant.now();
        }

        double ef = current != null && current.getEaseFactor() >= MIN_EASE_FACTOR ? current.getEaseFactor() : DEFAULT_EASE_FACTOR;
        int repCount = current != null ? current.getRepetitionCount() : 0;
        int intervalDays = current != null ? current.getIntervalDays() : 0;

        // Map ReviewRating to SM-2 quality grade (q in 1..5)
        int q;
        switch (rating) {
            case AGAIN:
                q = 1;
                break;
            case HARD:
                q = 3;
                break;
            case GOOD:
                q = 4;
                break;
            case EASY:
                q = 5;
                break;
            default:
                q = 4;
        }

        // 1. Calculate updated ease factor: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        double updatedEf = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (updatedEf < MIN_EASE_FACTOR) {
            updatedEf = MIN_EASE_FACTOR;
        }
        // Round to 3 decimal places for deterministic precision
        updatedEf = Math.round(updatedEf * 1000.0) / 1000.0;

        int nextInterval;
        int nextRepCount;
        ReviewState nextState;

        // 2. Compute repetition count and interval
        if (q < 3) {
            // AGAIN: reset repetition count, schedule for tomorrow (1 day)
            nextRepCount = 0;
            nextInterval = 1;
            nextState = ReviewState.LEARNING;
        } else {
            // Successful recall (HARD, GOOD, EASY)
            if (repCount == 0) {
                nextInterval = 1;
                nextRepCount = 1;
                nextState = ReviewState.LEARNING;
            } else if (repCount == 1) {
                if (q == 3) { // HARD
                    nextInterval = 3;
                } else { // GOOD or EASY
                    nextInterval = 6;
                }
                nextRepCount = 2;
                nextState = ReviewState.REVIEW;
            } else {
                if (q == 3) { // HARD: conservative 1.2x growth
                    nextInterval = Math.max(intervalDays + 1, (int) Math.round(intervalDays * 1.2));
                } else if (q == 4) { // GOOD: standard EF growth
                    nextInterval = Math.max(intervalDays + 1, (int) Math.round(intervalDays * updatedEf));
                } else { // EASY: accelerated bonus growth
                    nextInterval = Math.max(intervalDays + 2, (int) Math.round(intervalDays * updatedEf * 1.3));
                }
                nextRepCount = repCount + 1;
                nextState = nextRepCount >= 3 ? ReviewState.GRADUATED : ReviewState.REVIEW;
            }
        }

        Instant nextDueAt = now.plus(Duration.ofDays(nextInterval));

        return new ReviewCalculationResult(nextRepCount, updatedEf, nextInterval, nextDueAt, nextState);
    }
}

