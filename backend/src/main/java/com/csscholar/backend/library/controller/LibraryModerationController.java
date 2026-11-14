package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.library.dto.*;
import com.csscholar.backend.library.enums.ReportStatus;
import com.csscholar.backend.library.service.LibraryModerationService;
import com.csscholar.backend.library.service.LibraryReportService;
import com.csscholar.backend.library.service.LibraryResourceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/library/moderation")
@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
public class LibraryModerationController {

    private final LibraryModerationService moderationService;
    private final LibraryResourceService resourceService;
    private final LibraryReportService reportService;

    public LibraryModerationController(LibraryModerationService moderationService,
                                       LibraryResourceService resourceService,
                                       LibraryReportService reportService) {
        this.moderationService = moderationService;
        this.resourceService = resourceService;
        this.reportService = reportService;
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Page<LibraryResourceSummaryResponse>>> getModerationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<LibraryResourceSummaryResponse> queue = moderationService.getModerationQueue(page, size, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Moderation queue retrieved successfully.", queue));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> getResourceForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse detail = resourceService.getResourceDetail(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource review details retrieved successfully.", detail));
    }

    @PostMapping("/resources/{id}/approve")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> approveResource(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = moderationService.approveResource(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Resource approved and published successfully.", response));
    }

    @PostMapping("/resources/{id}/reject")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> rejectResource(
            @PathVariable UUID id,
            @RequestBody ModerationActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = moderationService.rejectResource(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Resource rejected with feedback provided to contributor.", response));
    }

    @PostMapping("/resources/{id}/suspend")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> suspendResource(
            @PathVariable UUID id,
            @RequestBody ModerationActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = moderationService.suspendResource(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Resource suspended from public library.", response));
    }

    @PostMapping("/resources/{id}/restore")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> restoreResource(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = moderationService.restoreResource(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Resource restored to public library.", response));
    }

    @GetMapping("/resources/{id}/logs")
    public ResponseEntity<ApiResponse<List<ModerationLogResponse>>> getResourceLogs(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ModerationLogResponse> logs = moderationService.getResourceModerationLogs(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource moderation audit logs retrieved successfully.", logs));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<ModerationLogResponse>>> getGlobalLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<ModerationLogResponse> logs = moderationService.getGlobalModerationLogs(page, size, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Global moderation logs retrieved successfully.", logs));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<ReportResponse> reports = reportService.getReports(status, page, size, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Moderation reports retrieved successfully.", reports));
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportResolutionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReportResponse response = reportService.resolveReport(reportId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Report resolution recorded successfully.", response));
    }
}

