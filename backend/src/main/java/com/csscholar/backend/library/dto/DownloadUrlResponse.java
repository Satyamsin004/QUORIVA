package com.csscholar.backend.library.dto;

public class DownloadUrlResponse {

    private String downloadUrl;
    private String fileName;
    private Long expiresInSeconds;

    public DownloadUrlResponse() {
    }

    public DownloadUrlResponse(String downloadUrl, String fileName, Long expiresInSeconds) {
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}

