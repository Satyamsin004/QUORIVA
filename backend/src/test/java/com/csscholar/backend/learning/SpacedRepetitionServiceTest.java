package com.csscholar.backend.learning;

import com.csscholar.backend.learning.entity.FlashcardReviewState;
import com.csscholar.backend.learning.enums.ReviewRating;
import com.csscholar.backend.learning.enums.ReviewState;
import com.csscholar.backend.learning.service.SpacedRepetitionService;
import com.csscholar.backend.learning.service.SpacedRepetitionService.ReviewCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpacedRepetitionService - SM-2 Algorithm Tests")
class SpacedRepetitionServiceTest {

    private SpacedRepetitionService sm2Service;

    @BeforeEach
    void setUp() {
        sm2Service = new SpacedRepetitionService();
    }

    private FlashcardReviewState createState(ReviewState state, int repetitionCount, double easeFactor, int intervalDays) {
        FlashcardReviewState s = new FlashcardReviewState();
        s.setState(state);
        s.setRepetitionCount(repetitionCount);
        s.setEaseFactor(easeFactor);
        s.setIntervalDays(intervalDays);
        return s;
    }

    @Test
    @DisplayName("Initial card rated GOOD should advance to 1 day interval with repetition count 1 and state LEARNING")
    void testInitialReviewGood() {
        Instant now = Instant.now();
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.NEW, 0, 2.5, 0),
                ReviewRating.GOOD,
                now
        );

        assertThat(calc.getRepetitionCount()).isEqualTo(1);
        assertThat(calc.getIntervalDays()).isEqualTo(1);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.LEARNING);
        // Formula for q=4: EF' = 2.5 + (0.1 - (5-4)*(0.08 + (5-4)*0.02)) = 2.5 + (0.1 - 0.10) = 2.50
        assertThat(calc.getEaseFactor()).isEqualTo(2.50);
        assertThat(calc.getNextDueAt()).isAfter(now.plus(23, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Second consecutive GOOD review should advance interval to 6 days and state to REVIEW")
    void testSecondReviewGood() {
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.LEARNING, 1, 2.5, 1),
                ReviewRating.GOOD,
                Instant.now()
        );

        assertThat(calc.getRepetitionCount()).isEqualTo(2);
        assertThat(calc.getIntervalDays()).isEqualTo(6);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.REVIEW);
        assertThat(calc.getEaseFactor()).isEqualTo(2.50);
    }

    @Test
    @DisplayName("Third consecutive GOOD review should advance interval by rounded (6 * EF) and state to GRADUATED")
    void testThirdReviewGood() {
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.REVIEW, 2, 2.5, 6),
                ReviewRating.GOOD,
                Instant.now()
        );

        assertThat(calc.getRepetitionCount()).isEqualTo(3);
        // 6 * 2.5 = 15
        assertThat(calc.getIntervalDays()).isEqualTo(15);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.GRADUATED);
        assertThat(calc.getEaseFactor()).isEqualTo(2.50);
    }

    @Test
    @DisplayName("AGAIN rating resets repetition count to 0 and resets interval to 1 day")
    void testAgainRatingResetsRepetition() {
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.GRADUATED, 3, 2.5, 15),
                ReviewRating.AGAIN,
                Instant.now()
        );

        assertThat(calc.getRepetitionCount()).isEqualTo(0);
        assertThat(calc.getIntervalDays()).isEqualTo(1);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.LEARNING);
        // Formula for q=1: 2.5 + (0.1 - 4*(0.08 + 4*0.02)) = 2.5 + (0.1 - 0.64) = 1.96
        assertThat(calc.getEaseFactor()).isLessThan(2.5);
    }

    @Test
    @DisplayName("Ease factor must never drop below 1.3 minimum floor")
    void testEaseFactorFloor() {
        // Start with EF = 1.4, test AGAIN review
        ReviewCalculationResult calc1 = sm2Service.calculate(
                createState(ReviewState.LEARNING, 1, 1.4, 1),
                ReviewRating.AGAIN,
                Instant.now()
        );

        // 1.4 - 0.54 = 0.86 -> clamped to 1.30
        assertThat(calc1.getEaseFactor()).isEqualTo(1.30);

        ReviewCalculationResult calc2 = sm2Service.calculate(
                createState(ReviewState.LEARNING, 0, 1.3, 1),
                ReviewRating.AGAIN,
                Instant.now()
        );

        assertThat(calc2.getEaseFactor()).isEqualTo(1.30);
    }

    @Test
    @DisplayName("EASY rating increases ease factor and grants interval bonus")
    void testEasyRatingIncreasesEaseFactor() {
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.REVIEW, 2, 2.5, 6),
                ReviewRating.EASY,
                Instant.now()
        );

        // For q=5: 2.5 + (0.1 - 0) = 2.60
        assertThat(calc.getEaseFactor()).isEqualTo(2.60);
        assertThat(calc.getRepetitionCount()).isEqualTo(3);
        // Bonus: 6 * 2.6 * 1.3 = 20.28 -> 20 days
        assertThat(calc.getIntervalDays()).isEqualTo(20);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.GRADUATED);
    }

    @Test
    @DisplayName("HARD rating advances repetition count to 3, reduces EF and gives modest interval growth")
    void testHardRating() {
        ReviewCalculationResult calc = sm2Service.calculate(
                createState(ReviewState.REVIEW, 2, 2.5, 6),
                ReviewRating.HARD,
                Instant.now()
        );

        // For q=3: 2.5 + (0.1 - 2*(0.08 + 2*0.02)) = 2.5 + (0.1 - 0.24) = 2.36
        assertThat(calc.getEaseFactor()).isEqualTo(2.36);
        assertThat(calc.getRepetitionCount()).isEqualTo(3);
        // Interval: max(7, round(6 * 1.2)) = 7
        assertThat(calc.getIntervalDays()).isEqualTo(7);
        assertThat(calc.getNewState()).isEqualTo(ReviewState.GRADUATED);
    }
}
