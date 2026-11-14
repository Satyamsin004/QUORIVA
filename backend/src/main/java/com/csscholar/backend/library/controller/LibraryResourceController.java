package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.library.dto.DownloadUrlResponse;
import com.csscholar.backend.library.dto.LibraryResourceDetailResponse;
import com.csscholar.backend.library.dto.LibraryResourceSummaryResponse;
import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
import com.csscholar.backend.library.service.LibraryResourceService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/library")
public class LibraryResourceController {

    private final LibraryResourceService resourceService;

    public LibraryResourceController(LibraryResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<Page<LibraryResourceSummaryResponse>>> searchResources(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AcademicCategory category,
            @RequestParam(required = false) LibraryResourceType resourceType,
            @RequestParam(required = false) AcademicLevel academicLevel,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "RECENT") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Page<LibraryResourceSummaryResponse> resources = resourceService.searchPublishedResources(
                q, category, resourceType, academicLevel, tag, sort, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully.", resources));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<LibraryResourceDetailResponse>> getResourceDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LibraryResourceDetailResponse detail = resourceService.getResourceDetail(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource details retrieved successfully.", detail));
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> downloadResource(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DownloadUrlResponse downloadUrl = resourceService.generateDownloadUrl(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Download URL generated successfully.", downloadUrl));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getCategories() {
        List<Map<String, String>> categories = Arrays.stream(AcademicCategory.values())
                .map(c -> Map.of("value", c.name(), "label", c.getDisplayName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully.", categories));
    }

    @GetMapping("/resource-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getResourceTypes() {
        List<Map<String, String>> types = Arrays.stream(LibraryResourceType.values())
                .map(t -> Map.of("value", t.name(), "label", t.getDisplayName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Resource types retrieved successfully.", types));
    }

    @GetMapping("/rights-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getRightsTypes() {
        List<Map<String, String>> types = Arrays.stream(RightsType.values())
                .map(r -> Map.of("value", r.name(), "label", r.getDisplayName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Rights types retrieved successfully.", types));
    }

    @GetMapping("/academic-levels")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAcademicLevels() {
        List<Map<String, String>> levels = Arrays.stream(AcademicLevel.values())
                .map(l -> Map.of("value", l.name(), "label", l.getDisplayName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Academic levels retrieved successfully.", levels));
    }

    @GetMapping("/tags/popular")
    public ResponseEntity<ApiResponse<List<String>>> getPopularTags(
            @RequestParam(defaultValue = "15") int limit
    ) {
        List<String> tags = resourceService.getPopularTags(limit);
        return ResponseEntity.ok(ApiResponse.success("Popular tags retrieved successfully.", tags));
    }
}

