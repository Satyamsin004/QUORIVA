package com.csscholar.backend.library.dto;

import java.util.UUID;

public class LibraryUploadSlotResponse {

    private UUID resourceId;
    private String storageKey;
    private String uploadUrl;
    private Long expiresInSeconds;

    public LibraryUploadSlotResponse() {
    }

    public LibraryUploadSlotResponse(UUID resourceId, String storageKey, String uploadUrl, Long expiresInSeconds) {
        this.resourceId = resourceId;
        this.storageKey = storageKey;
        this.uploadUrl = uploadUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}

