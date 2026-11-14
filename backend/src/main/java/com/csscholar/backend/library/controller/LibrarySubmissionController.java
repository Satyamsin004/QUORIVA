package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.library.dto.*;
import com.csscholar.backend.library.service.LibrarySubmissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/library")
public class LibrarySubmissionController {

    private final LibrarySubmissionService submissionService;

    public LibrarySubmissionController(LibrarySubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/submissions/upload-url")
    public ResponseEntity<ApiResponse<LibraryUploadSlotResponse>> requestUploadUrl(
            @Valid @RequestBody LibraryUploadSlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryUploadSlotResponse response = submissionService.createUploadSlot(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Upload URL created successfully.", response));
    }

    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> createSubmission(
            @Valid @RequestBody CreateLibrarySubmissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = submissionService.createSubmission(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission created successfully.", response));
    }

    @PutMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> updateSubmission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLibrarySubmissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = submissionService.updateSubmission(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Submission updated successfully.", response));
    }

    @PostMapping("/submissions/{id}/submit")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = submissionService.submitForReview(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource submitted for editorial review.", response));
    }

    @PostMapping("/submissions/{id}/withdraw")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> withdrawSubmission(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse response = submissionService.withdrawSubmission(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Submission withdrawn back to draft.", response));
    }

    @DeleteMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubmission(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        submissionService.deleteSubmission(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Submission deleted successfully.", null));
    }

    @GetMapping(path = {"/my-submissions", "/submissions/my"})
    public ResponseEntity<ApiResponse<Page<LibraryResourceSummaryResponse>>> getMySubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<LibraryResourceSummaryResponse> submissions = submissionService.getMySubmissions(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Your submissions retrieved successfully.", submissions));
    }
}

