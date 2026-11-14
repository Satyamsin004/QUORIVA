package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.library.dto.CreateReportRequest;
import com.csscholar.backend.library.dto.ReportResponse;
import com.csscholar.backend.library.service.LibraryReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/library/resources/{id}/reports")
public class LibraryReportController {

    private final LibraryReportService reportService;

    public LibraryReportController(LibraryReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> submitReport(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReportResponse response = reportService.createReport(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource report submitted for moderation review.", response));
    }
}

