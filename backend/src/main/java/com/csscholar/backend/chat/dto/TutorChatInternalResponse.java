package com.csscholar.backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TutorChatInternalResponse {

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("groundingStatus")
    @JsonAlias({"grounding_status", "groundingStatus"})
    private String groundingStatus; // "GROUNDED", "PARTIALLY_GROUNDED", "INSUFFICIENT_EVIDENCE"

    @JsonProperty("insufficientEvidence")
    @JsonAlias({"insufficient_evidence", "insufficientEvidence"})
    private boolean insufficientEvidence;

    @JsonProperty("citations")
    private List<TutorCitationDto> citations = new ArrayList<>();

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("modelName")
    @JsonAlias({"model_name", "modelName"})
    private String modelName;

    @JsonProperty("inputTokens")
    @JsonAlias({"input_tokens", "inputTokens"})
    private Integer inputTokens;

    @JsonProperty("outputTokens")
    @JsonAlias({"output_tokens", "outputTokens"})
    private Integer outputTokens;

    @JsonProperty("latencyMs")
    @JsonAlias({"latency_ms", "latencyMs"})
    private Long latencyMs;

    public TutorChatInternalResponse() {}

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getGroundingStatus() {
        return groundingStatus;
    }

    public void setGroundingStatus(String groundingStatus) {
        this.groundingStatus = groundingStatus;
    }

    public boolean isInsufficientEvidence() {
        return insufficientEvidence;
    }

    public void setInsufficientEvidence(boolean insufficientEvidence) {
        this.insufficientEvidence = insufficientEvidence;
    }

    public List<TutorCitationDto> getCitations() {
        return citations;
    }

    public void setCitations(List<TutorCitationDto> citations) {
        this.citations = citations;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}

