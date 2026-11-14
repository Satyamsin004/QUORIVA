package com.csscholar.backend.search.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.search.dto.SearchRequest;
import com.csscholar.backend.search.dto.SearchResponse;
import com.csscholar.backend.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SearchRequest request
    ) {
        SearchResponse response = searchService.search(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully.", response));
    }
}

