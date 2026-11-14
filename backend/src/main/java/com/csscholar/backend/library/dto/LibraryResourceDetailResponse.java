package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class LibraryResourceDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private AcademicCategory category;
    private String categoryDisplayName;
    private LibraryResourceType resourceType;
    private String resourceTypeDisplayName;
    private AcademicLevel academicLevel;
    private String academicLevelDisplayName;
    private String language;
    private UUID contributorId;
    private String contributorName;
    private String contributorEmail;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String checksumSha256;
    private Integer pageCount;
    private RightsType rightsType;
    private String rightsTypeDisplayName;
    private String licenseName;
    private String sourceAttribution;
    private LibraryResourceStatus status;
    private String rejectionReason;
    private Long downloadsCount;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private boolean canDownload;
    private boolean canEdit;
    private boolean canModerate;

    public LibraryResourceDetailResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public AcademicCategory getCategory() {
        return category;
    }

    public void setCategory(AcademicCategory category) {
        this.category = category;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public LibraryResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(LibraryResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceTypeDisplayName() {
        return resourceTypeDisplayName;
    }

    public void setResourceTypeDisplayName(String resourceTypeDisplayName) {
        this.resourceTypeDisplayName = resourceTypeDisplayName;
    }

    public AcademicLevel getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(AcademicLevel academicLevel) {
        this.academicLevel = academicLevel;
    }

    public String getAcademicLevelDisplayName() {
        return academicLevelDisplayName;
    }

    public void setAcademicLevelDisplayName(String academicLevelDisplayName) {
        this.academicLevelDisplayName = academicLevelDisplayName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public UUID getContributorId() {
        return contributorId;
    }

    public void setContributorId(UUID contributorId) {
        this.contributorId = contributorId;
    }

    public String getContributorName() {
        return contributorName;
    }

    public void setContributorName(String contributorName) {
        this.contributorName = contributorName;
    }

    public String getContributorEmail() {
        return contributorEmail;
    }

    public void setContributorEmail(String contributorEmail) {
        this.contributorEmail = contributorEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
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

    public RightsType getRightsType() {
        return rightsType;
    }

    public void setRightsType(RightsType rightsType) {
        this.rightsType = rightsType;
    }

    public String getRightsTypeDisplayName() {
        return rightsTypeDisplayName;
    }

    public void setRightsTypeDisplayName(String rightsTypeDisplayName) {
        this.rightsTypeDisplayName = rightsTypeDisplayName;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }

    public LibraryResourceStatus getStatus() {
        return status;
    }

    public void setStatus(LibraryResourceStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getDownloadsCount() {
        return downloadsCount;
    }

    public void setDownloadsCount(Long downloadsCount) {
        this.downloadsCount = downloadsCount;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isCanDownload() {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload) {
        this.canDownload = canDownload;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanModerate() {
        return canModerate;
    }

    public void setCanModerate(boolean canModerate) {
        this.canModerate = canModerate;
    }
}

