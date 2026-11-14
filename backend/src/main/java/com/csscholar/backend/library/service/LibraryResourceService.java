package com.csscholar.backend.library.service;

import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.library.dto.DownloadUrlResponse;
import com.csscholar.backend.library.dto.LibraryResourceDetailResponse;
import com.csscholar.backend.library.dto.LibraryResourceSummaryResponse;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.entity.LibraryResourceTag;
import com.csscholar.backend.library.enums.*;
import com.csscholar.backend.library.repository.LibraryResourceRepository;
import com.csscholar.backend.library.repository.LibraryResourceTagRepository;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LibraryResourceService {

    private final LibraryResourceRepository resourceRepository;
    private final LibraryResourceTagRepository tagRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;

    public LibraryResourceService(LibraryResourceRepository resourceRepository,
                                  LibraryResourceTagRepository tagRepository,
                                  UserRepository userRepository,
                                  ObjectStorageService objectStorageService) {
        this.resourceRepository = resourceRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional(readOnly = true)
    public Page<LibraryResourceSummaryResponse> searchPublishedResources(
            String query,
            AcademicCategory category,
            LibraryResourceType resourceType,
            AcademicLevel academicLevel,
            String tag,
            String sortBy,
            int page,
            int size
    ) {
        Sort sort = switch (sortBy != null ? sortBy.toUpperCase() : "RECENT") {
            case "POPULAR" -> Sort.by(Sort.Direction.DESC, "downloadsCount", "createdAt");
            case "TITLE" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt");
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), sort);

        Specification<LibraryResource> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), LibraryResourceStatus.PUBLISHED));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }

            if (academicLevel != null && academicLevel != AcademicLevel.ALL_LEVELS) {
                predicates.add(cb.or(
                        cb.equal(root.get("academicLevel"), academicLevel),
                        cb.equal(root.get("academicLevel"), AcademicLevel.ALL_LEVELS)
                ));
            }

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate attrMatch = cb.like(cb.lower(root.get("sourceAttribution")), pattern);
                predicates.add(cb.or(titleMatch, descMatch, attrMatch));
            }

            if (tag != null && !tag.trim().isEmpty()) {
                Join<LibraryResource, LibraryResourceTag> tagJoin = root.join("tags");
                predicates.add(cb.equal(cb.lower(tagJoin.get("tag")), tag.trim().toLowerCase()));
                if (cq != null) {
                    cq.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return resourceRepository.findAll(spec, pageable).map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public LibraryResourceDetailResponse getResourceDetail(UUID resourceId, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Library resource not found: " + resourceId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        boolean isContributor = resource.getContributor().getId().equals(currentUser.getId());
        boolean isModerator = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.EDUCATOR;
        boolean isPublished = resource.getStatus() == LibraryResourceStatus.PUBLISHED;

        if (!isPublished && !isContributor && !isModerator) {
            throw new ForbiddenException("You do not have permission to view this unpublished resource.");
        }

        LibraryResourceDetailResponse response = toDetailResponse(resource);
        response.setCanDownload(isPublished || isContributor || isModerator);
        response.setCanEdit(isContributor && (resource.getStatus() == LibraryResourceStatus.DRAFT || resource.getStatus() == LibraryResourceStatus.REJECTED));
        response.setCanModerate(isModerator);

        return response;
    }

    @Transactional
    public DownloadUrlResponse generateDownloadUrl(UUID resourceId, UUID currentUserId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Library resource not found: " + resourceId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        boolean isContributor = resource.getContributor().getId().equals(currentUser.getId());
        boolean isModerator = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.EDUCATOR;
        boolean isPublished = resource.getStatus() == LibraryResourceStatus.PUBLISHED;

        if (!isPublished && !isContributor && !isModerator) {
            throw new ForbiddenException("Download forbidden: this resource is not published.");
        }

        // Increment download counter for published resources
        if (isPublished) {
            resourceRepository.incrementDownloadCount(resourceId);
        }

        Duration expiration = Duration.ofMinutes(15);
        String downloadUrl = objectStorageService.generatePresignedDownloadUrl(
                resource.getStorageKey(),
                expiration,
                resource.getFileName()
        );

        return new DownloadUrlResponse(downloadUrl, resource.getFileName(), expiration.toSeconds());
    }

    @Transactional(readOnly = true)
    public List<String> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(50, limit)));
        return tagRepository.findPopularTags(pageable);
    }

    public LibraryResourceSummaryResponse toSummaryResponse(LibraryResource resource) {
        LibraryResourceSummaryResponse dto = new LibraryResourceSummaryResponse();
        dto.setId(resource.getId());
        dto.setTitle(resource.getTitle());
        dto.setDescription(resource.getDescription());
        dto.setCategory(resource.getCategory());
        dto.setCategoryDisplayName(resource.getCategory() != null ? resource.getCategory().getDisplayName() : null);
        dto.setResourceType(resource.getResourceType());
        dto.setResourceTypeDisplayName(resource.getResourceType() != null ? resource.getResourceType().getDisplayName() : null);
        dto.setAcademicLevel(resource.getAcademicLevel());
        dto.setAcademicLevelDisplayName(resource.getAcademicLevel() != null ? resource.getAcademicLevel().getDisplayName() : null);
        dto.setLanguage(resource.getLanguage());
        dto.setContributorId(resource.getContributor().getId());
        dto.setContributorName(resource.getContributor().getFullName() != null ? resource.getContributor().getFullName() : resource.getContributor().getEmail());
        dto.setFileName(resource.getFileName());
        dto.setFileType(resource.getFileType());
        dto.setFileSize(resource.getFileSize());
        dto.setPageCount(resource.getPageCount());
        dto.setRightsType(resource.getRightsType());
        dto.setRightsTypeDisplayName(resource.getRightsType() != null ? resource.getRightsType().getDisplayName() : null);
        dto.setLicenseName(resource.getLicenseName());
        dto.setStatus(resource.getStatus());
        dto.setDownloadsCount(resource.getDownloadsCount());
        dto.setCreatedAt(resource.getCreatedAt());
        dto.setPublishedAt(resource.getPublishedAt());
        dto.setTags(resource.getTags().stream().map(LibraryResourceTag::getTag).collect(Collectors.toSet()));
        return dto;
    }

    public LibraryResourceDetailResponse toDetailResponse(LibraryResource resource) {
        LibraryResourceDetailResponse dto = new LibraryResourceDetailResponse();
        dto.setId(resource.getId());
        dto.setTitle(resource.getTitle());
        dto.setDescription(resource.getDescription());
        dto.setCategory(resource.getCategory());
        dto.setCategoryDisplayName(resource.getCategory() != null ? resource.getCategory().getDisplayName() : null);
        dto.setResourceType(resource.getResourceType());
        dto.setResourceTypeDisplayName(resource.getResourceType() != null ? resource.getResourceType().getDisplayName() : null);
        dto.setAcademicLevel(resource.getAcademicLevel());
        dto.setAcademicLevelDisplayName(resource.getAcademicLevel() != null ? resource.getAcademicLevel().getDisplayName() : null);
        dto.setLanguage(resource.getLanguage());
        dto.setContributorId(resource.getContributor().getId());
        dto.setContributorName(resource.getContributor().getFullName() != null ? resource.getContributor().getFullName() : resource.getContributor().getEmail());
        dto.setContributorEmail(resource.getContributor().getEmail());
        dto.setFileName(resource.getFileName());
        dto.setFileType(resource.getFileType());
        dto.setMimeType(resource.getMimeType());
        dto.setFileSize(resource.getFileSize());
        dto.setChecksumSha256(resource.getChecksumSha256());
        dto.setPageCount(resource.getPageCount());
        dto.setRightsType(resource.getRightsType());
        dto.setRightsTypeDisplayName(resource.getRightsType() != null ? resource.getRightsType().getDisplayName() : null);
        dto.setLicenseName(resource.getLicenseName());
        dto.setSourceAttribution(resource.getSourceAttribution());
        dto.setStatus(resource.getStatus());
        dto.setRejectionReason(resource.getRejectionReason());
        dto.setDownloadsCount(resource.getDownloadsCount());
        dto.setCreatedAt(resource.getCreatedAt());
        dto.setUpdatedAt(resource.getUpdatedAt());
        dto.setPublishedAt(resource.getPublishedAt());
        dto.setTags(resource.getTags().stream().map(LibraryResourceTag::getTag).collect(Collectors.toSet()));
        return dto;
    }
}

