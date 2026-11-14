package com.csscholar.backend.library.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.DuplicateResourceException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.library.dto.*;
import com.csscholar.backend.library.entity.LibraryModerationLog;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.enums.ModerationAction;
import com.csscholar.backend.library.enums.RightsType;
import com.csscholar.backend.library.repository.LibraryModerationLogRepository;
import com.csscholar.backend.library.repository.LibraryResourceRepository;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.storage.StorageObjectMetadata;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class LibrarySubmissionService {

    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("PDF", "DOCX", "PPTX", "TXT");
    private static final String STORAGE_KEY_PATTERN = "^library/[0-9a-fA-F-]{36}/[a-zA-Z0-9._-]+$";

    private final LibraryResourceRepository resourceRepository;
    private final LibraryModerationLogRepository moderationLogRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final LibraryResourceService resourceService;

    public LibrarySubmissionService(LibraryResourceRepository resourceRepository,
                                    LibraryModerationLogRepository moderationLogRepository,
                                    UserRepository userRepository,
                                    ObjectStorageService objectStorageService,
                                    LibraryResourceService resourceService) {
        this.resourceRepository = resourceRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
        this.resourceService = resourceService;
    }

    public LibraryUploadSlotResponse createUploadSlot(LibraryUploadSlotRequest request, UUID currentUserId) {
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        if (request.getFileSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds 50MB limit.");
        }

        String fileTypeUpper = request.getFileType().trim().toUpperCase();
        if (!ALLOWED_FILE_TYPES.contains(fileTypeUpper)) {
            throw new BadRequestException("Unsupported file type: " + request.getFileType() + ". Allowed: PDF, DOCX, PPTX, TXT.");
        }

        UUID resourceId = UUID.randomUUID();
        String sanitizedFilename = request.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        // Enforce strict dedicated storage prefix 'library/{resourceId}/...'
        String storageKey = "library/" + resourceId + "/" + sanitizedFilename;

        Duration expiration = Duration.ofMinutes(15);
        String uploadUrl = objectStorageService.generatePresignedUploadUrl(
                storageKey,
                request.getContentType(),
                request.getFileSize(),
                expiration
        );

        return new LibraryUploadSlotResponse(resourceId, storageKey, uploadUrl, expiration.toSeconds());
    }

    @Transactional
    public LibraryResourceDetailResponse createSubmission(CreateLibrarySubmissionRequest request, UUID currentUserId) {
        User contributor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        // 1. Rights declaration validation
        if (request.getRightsType() == null) {
            throw new BadRequestException("A valid rights declaration is mandatory.");
        }
        if (request.getLicenseName() == null || request.getLicenseName().trim().isEmpty()) {
            throw new BadRequestException("License name must be specified.");
        }
        if (request.getRightsType() == RightsType.OTHER) {
            if (request.getSourceAttribution() == null || request.getSourceAttribution().trim().isEmpty()) {
                throw new BadRequestException("Source attribution and authorization basis must be specified when using 'OTHER' rights classification.");
            }
        }
        if (!Boolean.TRUE.equals(request.getWarrantyAgreed())) {
            throw new BadRequestException("Contributor must explicitly confirm the copyright and rights declaration warranty.");
        }

        // 2. Storage key isolation and path traversal defense
        String storageKey = request.getStorageKey();
        if (storageKey == null || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            throw new BadRequestException("Invalid storage key format. Storage keys must follow 'library/{resourceId}/{fileName}'.");
        }
        if (resourceRepository.findByStorageKey(storageKey).isPresent()) {
            throw new DuplicateResourceException("A library resource with this storage key already exists.");
        }

        // 3. File type check
        String fileTypeUpper = request.getFileType().trim().toUpperCase();
        if (!ALLOWED_FILE_TYPES.contains(fileTypeUpper)) {
            throw new BadRequestException("Unsupported file type: " + request.getFileType());
        }

        // 4. Object existence and integrity verification
        if (!objectStorageService.objectExists(storageKey)) {
            throw new BadRequestException("Resource file object was not found in storage. Upload must be completed before submission.");
        }
        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(storageKey);
        if (metadata == null || metadata.getContentLength() <= 0) {
            throw new BadRequestException("Uploaded resource file is empty or corrupted.");
        }

        LibraryResource resource = new LibraryResource();
        if (request.getId() != null) {
            resource.setId(request.getId());
        }
        resource.setTitle(request.getTitle().trim());
        resource.setDescription(request.getDescription().trim());
        resource.setCategory(request.getCategory());
        resource.setResourceType(request.getResourceType());
        resource.setAcademicLevel(request.getAcademicLevel() != null ? request.getAcademicLevel() : com.csscholar.backend.library.enums.AcademicLevel.ALL_LEVELS);
        resource.setLanguage(request.getLanguage() != null && !request.getLanguage().trim().isEmpty() ? request.getLanguage().trim() : "en");
        resource.setContributor(contributor);
        resource.setStorageKey(storageKey);
        resource.setFileName(request.getFileName().trim());
        resource.setFileType(fileTypeUpper);
        resource.setMimeType(request.getMimeType());
        resource.setFileSize(metadata.getContentLength());
        resource.setPageCount(request.getPageCount());
        resource.setRightsType(request.getRightsType());
        resource.setLicenseName(request.getLicenseName().trim());
        resource.setSourceAttribution(request.getSourceAttribution() != null ? request.getSourceAttribution().trim() : null);
        resource.setUploadedAt(Instant.now());

        boolean submitNow = request.getSubmitForReview();
        if (submitNow) {
            resource.setStatus(LibraryResourceStatus.PENDING_REVIEW);
        } else {
            resource.setStatus(LibraryResourceStatus.DRAFT);
        }

        if (request.getTags() != null) {
            request.getTags().forEach(resource::addTag);
        }

        LibraryResource saved = resourceRepository.save(resource);

        if (submitNow) {
            LibraryModerationLog log = new LibraryModerationLog(
                    saved,
                    contributor,
                    ModerationAction.SUBMITTED,
                    LibraryResourceStatus.DRAFT,
                    LibraryResourceStatus.PENDING_REVIEW,
                    "Initial submission for review",
                    "Submitted by contributor with legal warranty declaration"
            );
            moderationLogRepository.save(log);
        }

        return resourceService.getResourceDetail(saved.getId(), currentUserId);
    }

    @Transactional
    public LibraryResourceDetailResponse updateSubmission(UUID resourceId, UpdateLibrarySubmissionRequest request, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (!resource.getContributor().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the contributor can update this submission.");
        }

        if (resource.getStatus() != LibraryResourceStatus.DRAFT && resource.getStatus() != LibraryResourceStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED submissions can be modified.");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            resource.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            resource.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            resource.setCategory(request.getCategory());
        }
        if (request.getResourceType() != null) {
            resource.setResourceType(request.getResourceType());
        }
        if (request.getAcademicLevel() != null) {
            resource.setAcademicLevel(request.getAcademicLevel());
        }
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            resource.setLanguage(request.getLanguage().trim());
        }
        if (request.getRightsType() != null) {
            resource.setRightsType(request.getRightsType());
        }
        if (request.getLicenseName() != null && !request.getLicenseName().trim().isEmpty()) {
            resource.setLicenseName(request.getLicenseName().trim());
        }
        if (request.getSourceAttribution() != null) {
            resource.setSourceAttribution(request.getSourceAttribution().trim());
        }

        // Validate OTHER classification in updates
        if (resource.getRightsType() == RightsType.OTHER) {
            if (resource.getSourceAttribution() == null || resource.getSourceAttribution().trim().isEmpty()) {
                throw new BadRequestException("Source attribution and authorization basis must be specified when using 'OTHER' rights classification.");
            }
        }

        if (request.getTags() != null) {
            resource.clearTags();
            request.getTags().forEach(resource::addTag);
        }

        LibraryResource updated = resourceRepository.save(resource);
        return resourceService.getResourceDetail(updated.getId(), currentUserId);
    }

    @Transactional
    public LibraryResourceDetailResponse submitForReview(UUID resourceId, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (!resource.getContributor().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the contributor can submit this resource for review.");
        }

        if (resource.getStatus() == LibraryResourceStatus.PENDING_REVIEW || resource.getStatus() == LibraryResourceStatus.PUBLISHED) {
            throw new BadRequestException("Resource is already in review or published.");
        }

        if (resource.getStatus() != LibraryResourceStatus.DRAFT && resource.getStatus() != LibraryResourceStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED submissions can be submitted for review.");
        }

        // Verify storage object existence before submission
        if (!objectStorageService.objectExists(resource.getStorageKey())) {
            throw new BadRequestException("Cannot submit resource: uploaded file is missing from storage.");
        }

        // Verify rights & attribution
        if (resource.getRightsType() == RightsType.OTHER) {
            if (resource.getSourceAttribution() == null || resource.getSourceAttribution().trim().isEmpty()) {
                throw new BadRequestException("Source attribution is required for 'OTHER' rights classification.");
            }
        }

        LibraryResourceStatus previousStatus = resource.getStatus();
        resource.setStatus(LibraryResourceStatus.PENDING_REVIEW);
        resource.setRejectionReason(null);
        LibraryResource saved = resourceRepository.save(resource);

        User contributor = resource.getContributor();
        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                contributor,
                ModerationAction.SUBMITTED,
                previousStatus,
                LibraryResourceStatus.PENDING_REVIEW,
                "Submitted for editorial moderation",
                "Rights declaration confirmed: " + resource.getRightsType() + " (" + resource.getLicenseName() + ")"
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), currentUserId);
    }

    @Transactional
    public LibraryResourceDetailResponse withdrawSubmission(UUID resourceId, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (!resource.getContributor().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the contributor can withdraw this submission.");
        }

        if (resource.getStatus() != LibraryResourceStatus.PENDING_REVIEW) {
            throw new BadRequestException("Only PENDING_REVIEW submissions can be withdrawn to DRAFT.");
        }

        resource.setStatus(LibraryResourceStatus.DRAFT);
        LibraryResource saved = resourceRepository.save(resource);

        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                resource.getContributor(),
                ModerationAction.SUBMITTED,
                LibraryResourceStatus.PENDING_REVIEW,
                LibraryResourceStatus.DRAFT,
                "Withdrawn by contributor",
                null
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), currentUserId);
    }

    @Transactional
    public void deleteSubmission(UUID resourceId, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (!resource.getContributor().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the contributor can delete this submission.");
        }

        if (resource.getStatus() != LibraryResourceStatus.DRAFT && resource.getStatus() != LibraryResourceStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED submissions can be deleted.");
        }

        try {
            objectStorageService.deleteObject(resource.getStorageKey());
        } catch (Exception ignored) {
        }

        // If the resource has moderation audit logs, DB trigger prohibits deleting audit logs.
        // We soft-delete by transitioning status to REMOVED, safely preserving audit immutability.
        boolean hasAuditLogs = moderationLogRepository.existsByResourceId(resourceId);
        if (hasAuditLogs) {
            resource.setStatus(LibraryResourceStatus.REMOVED);
            resourceRepository.save(resource);
        } else {
            resourceRepository.delete(resource);
        }
    }

    @Transactional(readOnly = true)
    public Page<LibraryResourceSummaryResponse> getMySubmissions(UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        return resourceRepository.findByContributorIdAndStatusNotOrderByCreatedAtDesc(currentUserId, LibraryResourceStatus.REMOVED, pageable)
                .map(resourceService::toSummaryResponse);
    }
}


