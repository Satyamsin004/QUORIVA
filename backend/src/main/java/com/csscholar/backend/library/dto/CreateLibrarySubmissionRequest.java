package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public class CreateLibrarySubmissionRequest {

    private UUID id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Academic category is required")
    private AcademicCategory category;

    @NotNull(message = "Resource type is required")
    private LibraryResourceType resourceType;

    private AcademicLevel academicLevel = AcademicLevel.ALL_LEVELS;

    private String language = "en";

    @NotBlank(message = "Storage key is required")
    private String storageKey;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type is required")
    private String fileType;

    @NotBlank(message = "MIME type is required")
    private String mimeType;

    @NotNull(message = "File size is required")
    private Long fileSize;

    private Integer pageCount;

    @NotNull(message = "Rights declaration is mandatory")
    private RightsType rightsType;

    @NotBlank(message = "License specification is mandatory")
    private String licenseName;

    private String sourceAttribution;

    private Set<String> tags;

    @NotNull(message = "Contributor copyright warranty agreement is mandatory")
    private Boolean warrantyAgreed;

    private Boolean submitForReview = false;

    public CreateLibrarySubmissionRequest() {
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

    public LibraryResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(LibraryResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public AcademicLevel getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(AcademicLevel academicLevel) {
        this.academicLevel = academicLevel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Boolean getWarrantyAgreed() {
        return warrantyAgreed != null && warrantyAgreed;
    }

    public void setWarrantyAgreed(Boolean warrantyAgreed) {
        this.warrantyAgreed = warrantyAgreed;
    }

    public Boolean getSubmitForReview() {
        return submitForReview != null && submitForReview;
    }

    public void setSubmitForReview(Boolean submitForReview) {
        this.submitForReview = submitForReview;
    }
}

