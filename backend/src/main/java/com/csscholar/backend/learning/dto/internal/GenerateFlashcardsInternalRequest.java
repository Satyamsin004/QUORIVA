package com.csscholar.backend.learning.dto.internal;

import java.util.UUID;

public class GenerateFlashcardsInternalRequest {

    private UUID userId;
    private String sourceType;
    private String sourceTitle;
    private String contextText;
    private int cardCount;
    private String conceptFocus;

    public GenerateFlashcardsInternalRequest() {}

    public GenerateFlashcardsInternalRequest(UUID userId, String sourceType, String sourceTitle,
                                             String contextText, int cardCount, String conceptFocus) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceTitle = sourceTitle;
        this.contextText = contextText;
        this.cardCount = cardCount;
        this.conceptFocus = conceptFocus;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }

    public String getContextText() { return contextText; }
    public void setContextText(String contextText) { this.contextText = contextText; }

    public int getCardCount() { return cardCount; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }

    public String getConceptFocus() { return conceptFocus; }
    public void setConceptFocus(String conceptFocus) { this.conceptFocus = conceptFocus; }
}

