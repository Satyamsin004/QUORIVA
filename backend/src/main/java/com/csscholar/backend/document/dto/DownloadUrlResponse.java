package com.csscholar.backend.document.dto;

import java.util.UUID;

public class DownloadUrlResponse {

    private UUID documentId;
    private String downloadUrl;
    private long expiresInSeconds;

    public DownloadUrlResponse() {
    }

    public DownloadUrlResponse(UUID documentId, String downloadUrl, long expiresInSeconds) {
        this.documentId = documentId;
        this.downloadUrl = downloadUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}

