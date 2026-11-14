package com.csscholar.backend.search.service;

import com.csscholar.backend.search.client.SearchClient;
import com.csscholar.backend.search.dto.SearchRequest;
import com.csscholar.backend.search.dto.SearchResponse;
import com.csscholar.backend.workspace.WorkspaceResponse;
import com.csscholar.backend.workspace.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final WorkspaceService workspaceService;
    private final SearchClient searchClient;

    public SearchService(WorkspaceService workspaceService, SearchClient searchClient) {
        this.workspaceService = workspaceService;
        this.searchClient = searchClient;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(UUID userId, SearchRequest request) {
        UUID workspaceId = request.getWorkspaceId();

        // 1. Authorize: Verify user has access to the target workspace (throws ForbiddenException if unauthorized)
        WorkspaceResponse workspace = workspaceService.getWorkspaceById(userId, workspaceId);
        logger.info("Executing search for user: {}, workspace: '{}' ({})", userId, workspace.getName(), workspaceId);

        // 2. Delegate to internal AI retrieval engine
        return searchClient.executeSearch(
                request.getQuery().trim(),
                workspaceId,
                userId,
                request.getTopK()
        );
    }
}

