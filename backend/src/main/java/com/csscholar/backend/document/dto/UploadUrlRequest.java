package com.csscholar.backend.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UploadUrlRequest {

    @NotNull(message = "Workspace ID is required.")
    private UUID workspaceId;

    @NotBlank(message = "Filename is required.")
    @Size(max = 255, message = "Filename must not exceed 255 characters.")
    private String filename;

    @NotBlank(message = "Content-Type is required.")
    private String contentType;

    @NotNull(message = "File size is required.")
    @Positive(message = "File size must be greater than 0 bytes.")
    private Long fileSize;

    private String checksumSha256;

    @Size(max = 255, message = "Title must not exceed 255 characters.")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters.")
    private String description;

    @Size(max = 100, message = "Subject must not exceed 100 characters.")
    private String subject;

    public UploadUrlRequest() {
    }

    public UploadUrlRequest(UUID workspaceId, String filename, String contentType, Long fileSize,
                            String checksumSha256, String title, String description) {
        this.workspaceId = workspaceId;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.checksumSha256 = checksumSha256;
        this.title = title;
        this.description = description;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}

