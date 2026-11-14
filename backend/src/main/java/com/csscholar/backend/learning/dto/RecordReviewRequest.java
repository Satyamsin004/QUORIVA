package com.csscholar.backend.learning.dto;

import com.csscholar.backend.learning.enums.ReviewRating;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class RecordReviewRequest {

    @NotNull(message = "Card ID is required")
    private UUID cardId;

    @NotNull(message = "Review rating is required")
    private ReviewRating rating;

    private Integer reviewTimeSeconds = 0;

    public RecordReviewRequest() {}

    public RecordReviewRequest(UUID cardId, ReviewRating rating, Integer reviewTimeSeconds) {
        this.cardId = cardId;
        this.rating = rating;
        this.reviewTimeSeconds = reviewTimeSeconds;
    }

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }

    public ReviewRating getRating() { return rating; }
    public void setRating(ReviewRating rating) { this.rating = rating; }

    public Integer getReviewTimeSeconds() { return reviewTimeSeconds; }
    public void setReviewTimeSeconds(Integer reviewTimeSeconds) { this.reviewTimeSeconds = reviewTimeSeconds; }
}

