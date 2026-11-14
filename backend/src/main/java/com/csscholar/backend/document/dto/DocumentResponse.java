package com.csscholar.backend.document.dto;

import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.enums.DocumentVisibility;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.enums.ProcessingStatus;
import com.csscholar.backend.document.enums.ScanStatus;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {

    private UUID id;
    private UUID workspaceId;
    private String workspaceName;
    private UUID ownerId;
    private String ownerName;
    private String title;
    private String originalFilename;
    private String storageKey;
    private FileType fileType;
    private String mimeType;
    private Long fileSize;
    private String checksumSha256;
    private Integer pageCount;
    private String description;
    private String subject;
    private ProcessingStatus processingStatus;
    private ScanStatus scanStatus;
    private String embeddingStatus;
    private DocumentVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant uploadedAt;

    public DocumentResponse() {
    }

    public static DocumentResponse fromEntity(Document doc) {
        DocumentResponse res = new DocumentResponse();
        res.setId(doc.getId());
        if (doc.getWorkspace() != null) {
            res.setWorkspaceId(doc.getWorkspace().getId());
            res.setWorkspaceName(doc.getWorkspace().getName());
        }
        if (doc.getOwner() != null) {
            res.setOwnerId(doc.getOwner().getId());
            res.setOwnerName(doc.getOwner().getFullName());
        }
        res.setTitle(doc.getTitle());
        res.setOriginalFilename(doc.getOriginalFilename());
        res.setStorageKey(doc.getStorageKey());
        res.setFileType(doc.getFileType());
        res.setMimeType(doc.getMimeType());
        res.setFileSize(doc.getFileSize());
        res.setChecksumSha256(doc.getChecksumSha256());
        res.setPageCount(doc.getPageCount());
        res.setDescription(doc.getDescription());
        res.setSubject(doc.getSubject() != null ? doc.getSubject() : "General");
        res.setProcessingStatus(doc.getProcessingStatus());
        res.setScanStatus(doc.getScanStatus());
        res.setEmbeddingStatus(doc.getEmbeddingStatus());
        res.setVisibility(doc.getVisibility());
        res.setCreatedAt(doc.getCreatedAt());
        res.setUpdatedAt(doc.getUpdatedAt());
        res.setUploadedAt(doc.getUploadedAt());
        return res;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
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

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(ScanStatus scanStatus) {
        this.scanStatus = scanStatus;
    }

    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

