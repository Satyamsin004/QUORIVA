package com.csscholar.backend.library.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.library.dto.LibraryResourceDetailResponse;
import com.csscholar.backend.library.dto.LibraryResourceSummaryResponse;
import com.csscholar.backend.library.dto.ModerationActionRequest;
import com.csscholar.backend.library.dto.ModerationLogResponse;
import com.csscholar.backend.library.entity.LibraryModerationLog;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.enums.ModerationAction;
import com.csscholar.backend.library.repository.LibraryModerationLogRepository;
import com.csscholar.backend.library.repository.LibraryResourceRepository;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LibraryModerationService {

    private final LibraryResourceRepository resourceRepository;
    private final LibraryModerationLogRepository moderationLogRepository;
    private final UserRepository userRepository;
    private final LibraryResourceService resourceService;

    public LibraryModerationService(LibraryResourceRepository resourceRepository,
                                    LibraryModerationLogRepository moderationLogRepository,
                                    UserRepository userRepository,
                                    LibraryResourceService resourceService) {
        this.resourceRepository = resourceRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
    }

    private User validateModerator(UUID moderatorId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + moderatorId));

        if (moderator.getRole() != Role.ADMIN && moderator.getRole() != Role.EDUCATOR) {
            throw new ForbiddenException("Only administrators and educators have access to library moderation.");
        }
        return moderator;
    }

    @Transactional(readOnly = true)
    public Page<LibraryResourceSummaryResponse> getModerationQueue(int page, int size, UUID moderatorId) {
        validateModerator(moderatorId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        return resourceRepository.findByStatusOrderByCreatedAtAsc(LibraryResourceStatus.PENDING_REVIEW, pageable)
                .map(resourceService::toSummaryResponse);
    }

    @Transactional
    public LibraryResourceDetailResponse approveResource(UUID resourceId, UUID moderatorId, ModerationActionRequest request) {
        User moderator = validateModerator(moderatorId);
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (resource.getStatus() != LibraryResourceStatus.PENDING_REVIEW) {
            throw new BadRequestException("Only PENDING_REVIEW resources can be approved.");
        }

        LibraryResourceStatus previousStatus = resource.getStatus();
        resource.setStatus(LibraryResourceStatus.PUBLISHED);
        resource.setPublishedAt(Instant.now());
        resource.setRejectionReason(null);
        LibraryResource saved = resourceRepository.save(resource);

        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Meets academic and licensing standards";
        String notes = (request != null) ? request.getNotes() : null;

        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                moderator,
                ModerationAction.APPROVED,
                previousStatus,
                LibraryResourceStatus.PUBLISHED,
                reason,
                notes
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), moderatorId);
    }

    @Transactional
    public LibraryResourceDetailResponse rejectResource(UUID resourceId, UUID moderatorId, ModerationActionRequest request) {
        User moderator = validateModerator(moderatorId);

        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("A detailed rejection reason is required.");
        }

        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (resource.getStatus() != LibraryResourceStatus.PENDING_REVIEW) {
            throw new BadRequestException("Only PENDING_REVIEW resources can be rejected.");
        }

        LibraryResourceStatus previousStatus = resource.getStatus();
        resource.setStatus(LibraryResourceStatus.REJECTED);
        resource.setRejectionReason(request.getReason().trim());
        LibraryResource saved = resourceRepository.save(resource);

        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                moderator,
                ModerationAction.REJECTED,
                previousStatus,
                LibraryResourceStatus.REJECTED,
                request.getReason().trim(),
                request.getNotes()
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), moderatorId);
    }

    @Transactional
    public LibraryResourceDetailResponse suspendResource(UUID resourceId, UUID moderatorId, ModerationActionRequest request) {
        User moderator = validateModerator(moderatorId);

        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("A detailed suspension reason is required.");
        }

        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (resource.getStatus() != LibraryResourceStatus.PUBLISHED) {
            throw new BadRequestException("Only PUBLISHED resources can be suspended.");
        }

        LibraryResourceStatus previousStatus = resource.getStatus();
        resource.setStatus(LibraryResourceStatus.SUSPENDED);
        resource.setRejectionReason(request.getReason().trim());
        LibraryResource saved = resourceRepository.save(resource);

        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                moderator,
                ModerationAction.SUSPENDED,
                previousStatus,
                LibraryResourceStatus.SUSPENDED,
                request.getReason().trim(),
                request.getNotes()
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), moderatorId);
    }

    @Transactional
    public LibraryResourceDetailResponse restoreResource(UUID resourceId, UUID moderatorId, ModerationActionRequest request) {
        User moderator = validateModerator(moderatorId);
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (resource.getStatus() != LibraryResourceStatus.SUSPENDED) {
            throw new BadRequestException("Only SUSPENDED resources can be restored.");
        }

        LibraryResourceStatus previousStatus = resource.getStatus();
        resource.setStatus(LibraryResourceStatus.PUBLISHED);
        resource.setRejectionReason(null);
        LibraryResource saved = resourceRepository.save(resource);

        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Restored after moderation review";
        String notes = (request != null) ? request.getNotes() : null;

        LibraryModerationLog log = new LibraryModerationLog(
                saved,
                moderator,
                ModerationAction.RESTORED,
                previousStatus,
                LibraryResourceStatus.PUBLISHED,
                reason,
                notes
        );
        moderationLogRepository.save(log);

        return resourceService.getResourceDetail(saved.getId(), moderatorId);
    }

    @Transactional(readOnly = true)
    public List<ModerationLogResponse> getResourceModerationLogs(UUID resourceId, UUID moderatorId) {
        validateModerator(moderatorId);
        return moderationLogRepository.findByResourceIdOrderByCreatedAtDesc(resourceId).stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ModerationLogResponse> getGlobalModerationLogs(int page, int size, UUID moderatorId) {
        validateModerator(moderatorId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        return moderationLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toLogResponse);
    }

    private ModerationLogResponse toLogResponse(LibraryModerationLog log) {
        ModerationLogResponse response = new ModerationLogResponse();
        response.setId(log.getId());
        response.setResourceId(log.getResource().getId());
        response.setResourceTitle(log.getResource().getTitle());
        response.setModeratorId(log.getModerator().getId());
        response.setModeratorName(log.getModerator().getFullName() != null ? log.getModerator().getFullName() : log.getModerator().getEmail());
        response.setAction(log.getAction());
        response.setPreviousStatus(log.getPreviousStatus());
        response.setNewStatus(log.getNewStatus());
        response.setReason(log.getReason());
        response.setNotes(log.getNotes());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}

