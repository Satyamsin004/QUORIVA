package com.csscholar.backend.document.dto;

import java.util.Map;
import java.util.UUID;

public class UploadUrlResponse {

    private UUID documentId;
    private String uploadUrl;
    private String storageKey;
    private long expiresInSeconds;
    private Map<String, String> requiredHeaders;

    public UploadUrlResponse() {
    }

    public UploadUrlResponse(UUID documentId, String uploadUrl, String storageKey, long expiresInSeconds, Map<String, String> requiredHeaders) {
        this.documentId = documentId;
        this.uploadUrl = uploadUrl;
        this.storageKey = storageKey;
        this.expiresInSeconds = expiresInSeconds;
        this.requiredHeaders = requiredHeaders;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public Map<String, String> getRequiredHeaders() {
        return requiredHeaders;
    }

    public void setRequiredHeaders(Map<String, String> requiredHeaders) {
        this.requiredHeaders = requiredHeaders;
    }
}

