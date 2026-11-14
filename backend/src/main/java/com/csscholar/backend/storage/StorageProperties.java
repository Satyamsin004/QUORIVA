package com.csscholar.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "storage.s3")
public class StorageProperties {

    private String endpoint = "http://localhost:9000";
    private String region = "us-east-1";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "cs-scholar-documents";
    private boolean pathStyleAccess = true;
    private long presignedUrlExpirationSeconds = 900; // 15 minutes
    private long maxFileSizeBytes = 52428800L; // 50 MB

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public long getPresignedUrlExpirationSeconds() {
        return presignedUrlExpirationSeconds;
    }

    public void setPresignedUrlExpirationSeconds(long presignedUrlExpirationSeconds) {
        this.presignedUrlExpirationSeconds = presignedUrlExpirationSeconds;
    }

    public Duration getPresignedUrlDuration() {
        return Duration.ofSeconds(presignedUrlExpirationSeconds);
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}

