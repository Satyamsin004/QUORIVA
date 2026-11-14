package com.csscholar.backend.document.dto;

import com.csscholar.backend.document.enums.ProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public class DocumentStatusResponse {

    private UUID documentId;
    private ProcessingStatus processingStatus;
    private String embeddingStatus;
    private Integer pageCount;
    private String errorMessage;
    private Instant uploadedAt;
    private Instant updatedAt;

    public DocumentStatusResponse() {
    }

    public DocumentStatusResponse(UUID documentId, ProcessingStatus processingStatus, Instant uploadedAt, Instant updatedAt) {
        this.documentId = documentId;
        this.processingStatus = processingStatus;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    public DocumentStatusResponse(UUID documentId, ProcessingStatus processingStatus, String embeddingStatus,
                                  Integer pageCount, String errorMessage, Instant uploadedAt, Instant updatedAt) {
        this.documentId = documentId;
        this.processingStatus = processingStatus;
        this.embeddingStatus = embeddingStatus;
        this.pageCount = pageCount;
        this.errorMessage = errorMessage;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

