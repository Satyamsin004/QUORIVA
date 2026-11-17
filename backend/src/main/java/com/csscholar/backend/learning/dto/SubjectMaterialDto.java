package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class SubjectMaterialDto {

    @JsonProperty("documentId")
    private UUID documentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("originalFilename")
    private String originalFilename;

    @JsonProperty("fileType")
    private String fileType;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("pageCount")
    private Integer pageCount;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("processingStatus")
    private String processingStatus;

    @JsonProperty("uploadedAt")
    private Instant uploadedAt;

    public SubjectMaterialDto() {}

    public SubjectMaterialDto(UUID documentId, String title, String originalFilename,
                              String fileType, Long fileSize, Integer pageCount,
                              String subject, String processingStatus, Instant uploadedAt) {
        this.documentId = documentId;
        this.title = title;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.pageCount = pageCount;
        this.subject = subject;
        this.processingStatus = processingStatus;
        this.uploadedAt = uploadedAt;
    }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
