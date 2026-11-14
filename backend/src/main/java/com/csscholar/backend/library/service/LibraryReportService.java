package com.csscholar.backend.library.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.library.dto.CreateReportRequest;
import com.csscholar.backend.library.dto.ReportResolutionRequest;
import com.csscholar.backend.library.dto.ReportResponse;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.entity.LibraryResourceReport;
import com.csscholar.backend.library.enums.ReportStatus;
import com.csscholar.backend.library.repository.LibraryResourceReportRepository;
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
import java.util.UUID;

@Service
public class LibraryReportService {

    private final LibraryResourceReportRepository reportRepository;
    private final LibraryResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public LibraryReportService(LibraryResourceReportRepository reportRepository,
                                LibraryResourceRepository resourceRepository,
                                UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    private User validateModerator(UUID moderatorId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + moderatorId));

        if (moderator.getRole() != Role.ADMIN && moderator.getRole() != Role.EDUCATOR) {
            throw new ForbiddenException("Only administrators and educators can review library reports.");
        }
        return moderator;
    }

    @Transactional
    public ReportResponse createReport(UUID resourceId, CreateReportRequest request, UUID reporterId) {
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reporterId));

        if (request.getReasonCategory() == null) {
            throw new BadRequestException("Reason category is required.");
        }
        if (request.getExplanation() == null || request.getExplanation().trim().isEmpty()) {
            throw new BadRequestException("Explanation is required.");
        }

        LibraryResourceReport report = new LibraryResourceReport(
                resource,
                reporter,
                request.getReasonCategory(),
                request.getExplanation().trim()
        );

        LibraryResourceReport saved = reportRepository.save(report);
        return toReportResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(ReportStatus status, int page, int size, UUID moderatorId) {
        validateModerator(moderatorId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));

        if (status != null) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::toReportResponse);
        } else {
            return reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                    .map(this::toReportResponse);
        }
    }

    @Transactional
    public ReportResponse resolveReport(UUID reportId, ReportResolutionRequest request, UUID moderatorId) {
        User moderator = validateModerator(moderatorId);

        LibraryResourceReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        if (request.getAction() != ReportStatus.RESOLVED && request.getAction() != ReportStatus.DISMISSED) {
            throw new BadRequestException("Action must be RESOLVED or DISMISSED.");
        }

        report.setStatus(request.getAction());
        report.setResolutionNotes(request.getResolutionNotes() != null ? request.getResolutionNotes().trim() : null);
        report.setResolvedBy(moderator);
        report.setResolvedAt(Instant.now());

        LibraryResourceReport saved = reportRepository.save(report);
        return toReportResponse(saved);
    }

    public ReportResponse toReportResponse(LibraryResourceReport report) {
        ReportResponse dto = new ReportResponse();
        dto.setId(report.getId());
        dto.setResourceId(report.getResource().getId());
        dto.setResourceTitle(report.getResource().getTitle());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterName(report.getReporter().getFullName() != null ? report.getReporter().getFullName() : report.getReporter().getEmail());
        dto.setReasonCategory(report.getReasonCategory());
        dto.setReasonCategoryDisplayName(report.getReasonCategory() != null ? report.getReasonCategory().getDisplayName() : null);
        dto.setExplanation(report.getExplanation());
        dto.setStatus(report.getStatus());
        dto.setResolutionNotes(report.getResolutionNotes());
        if (report.getResolvedBy() != null) {
            dto.setResolvedById(report.getResolvedBy().getId());
            dto.setResolvedByName(report.getResolvedBy().getFullName() != null ? report.getResolvedBy().getFullName() : report.getResolvedBy().getEmail());
        }
        dto.setCreatedAt(report.getCreatedAt());
        dto.setResolvedAt(report.getResolvedAt());
        return dto;
    }
}

