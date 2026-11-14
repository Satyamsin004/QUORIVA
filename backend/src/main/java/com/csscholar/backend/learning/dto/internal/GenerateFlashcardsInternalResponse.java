package com.csscholar.backend.learning.dto.internal;

import java.util.ArrayList;
import java.util.List;

public class GenerateFlashcardsInternalResponse {

    private String title;
    private List<GeneratedFlashcardDto> cards = new ArrayList<>();
    private String provider;
    private String modelName;
    private int inputTokens;
    private int outputTokens;
    private int latencyMs;

    public GenerateFlashcardsInternalResponse() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<GeneratedFlashcardDto> getCards() { return cards; }
    public void setCards(List<GeneratedFlashcardDto> cards) { this.cards = cards; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
}

