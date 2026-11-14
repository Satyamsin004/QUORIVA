package com.csscholar.backend.library.entity;

import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
import com.csscholar.backend.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "library_resources")
public class LibraryResource {

    @Id
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 100)
    private AcademicCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 100)
    private LibraryResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_level", nullable = false, length = 100)
    private AcademicLevel academicLevel = AcademicLevel.ALL_LEVELS;

    @Column(name = "language", nullable = false, length = 20)
    private String language = "en";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private User contributor;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "rights_type", nullable = false, length = 100)
    private RightsType rightsType;

    @Column(name = "license_name", nullable = false, length = 255)
    private String licenseName;

    @Column(name = "source_attribution", columnDefinition = "TEXT")
    private String sourceAttribution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private LibraryResourceStatus status = LibraryResourceStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "downloads_count", nullable = false)
    private Long downloadsCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LibraryResourceTag> tags = new HashSet<>();

    public LibraryResource() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.downloadsCount == null) {
            this.downloadsCount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            LibraryResourceTag resourceTag = new LibraryResourceTag(this, tag.trim().toLowerCase());
            this.tags.add(resourceTag);
        }
    }

    public void clearTags() {
        this.tags.clear();
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

    public User getContributor() {
        return contributor;
    }

    public void setContributor(User contributor) {
        this.contributor = contributor;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Set<LibraryResourceTag> getTags() {
        return tags;
    }

    public void setTags(Set<LibraryResourceTag> tags) {
        this.tags = tags;
    }
}

