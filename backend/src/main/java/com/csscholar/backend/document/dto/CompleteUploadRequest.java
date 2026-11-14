package com.csscholar.backend.document.dto;

public class CompleteUploadRequest {

    private String checksumSha256;

    public CompleteUploadRequest() {
    }

    public CompleteUploadRequest(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }
}

