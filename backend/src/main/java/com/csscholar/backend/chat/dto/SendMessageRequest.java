package com.csscholar.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {

    @NotBlank(message = "Message content must not be blank")
    private String content;

    private String provider; // optional: "groq" or "gemini"

    private String modelName; // optional override

    public SendMessageRequest() {}

    public SendMessageRequest(String content) {
        this.content = content;
    }

    public SendMessageRequest(String content, String provider, String modelName) {
        this.content = content;
        this.provider = provider;
        this.modelName = modelName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}

