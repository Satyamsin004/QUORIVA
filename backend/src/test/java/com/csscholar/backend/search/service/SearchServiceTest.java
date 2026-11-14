package com.csscholar.backend.search.service;

import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.common.SearchException;
import com.csscholar.backend.search.client.SearchClient;
import com.csscholar.backend.search.dto.SearchRequest;
import com.csscholar.backend.search.dto.SearchResponse;
import com.csscholar.backend.search.dto.SearchResultDto;
import com.csscholar.backend.workspace.WorkspaceResponse;
import com.csscholar.backend.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private SearchClient searchClient;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(workspaceService, searchClient);
    }

    @Test
    @DisplayName("search should verify workspace membership and return search results")
    void search_Success() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        SearchRequest request = new SearchRequest("binary search tree", workspaceId, 10);

        WorkspaceResponse mockWorkspace = new WorkspaceResponse(
                workspaceId, "CS Data Structures", "Notes", userId, "Alice", Instant.now(), Instant.now()
        );
        when(workspaceService.getWorkspaceById(userId, workspaceId)).thenReturn(mockWorkspace);

        SearchResultDto item = new SearchResultDto();
        item.setChunkId(UUID.randomUUID());
        item.setDocumentId(UUID.randomUUID());
        item.setDocumentName("Algorithms.pdf");
        item.setWorkspaceId(workspaceId);
        item.setChunkContent("BST search time complexity is O(log n).");
        item.setFinalRank(1);
        item.setRerankerScore(0.95);
        item.setRrfScore(0.032);

        SearchResponse mockResponse = new SearchResponse("binary search tree", workspaceId, 1, Collections.singletonList(item));
        when(searchClient.executeSearch("binary search tree", workspaceId, userId, 10)).thenReturn(mockResponse);

        SearchResponse response = searchService.search(userId, request);

        assertNotNull(response);
        assertEquals("binary search tree", response.getQuery());
        assertEquals(workspaceId, response.getWorkspaceId());
        assertEquals(1, response.getTotalCandidates());
        assertEquals(1, response.getResults().size());
        assertEquals("Algorithms.pdf", response.getResults().get(0).getDocumentName());

        verify(workspaceService).getWorkspaceById(userId, workspaceId);
        verify(searchClient).executeSearch("binary search tree", workspaceId, userId, 10);
    }

    @Test
    @DisplayName("search should throw ForbiddenException when user does not have workspace access")
    void search_ForbiddenWorkspace_ThrowsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        SearchRequest request = new SearchRequest("binary search tree", workspaceId, 10);

        when(workspaceService.getWorkspaceById(userId, workspaceId))
                .thenThrow(new ForbiddenException("You do not have permission to access this workspace."));

        assertThrows(ForbiddenException.class, () -> searchService.search(userId, request));

        verify(workspaceService).getWorkspaceById(userId, workspaceId);
        verifyNoInteractions(searchClient);
    }

    @Test
    @DisplayName("search should throw ResourceNotFoundException when workspace does not exist")
    void search_WorkspaceNotFound_ThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        SearchRequest request = new SearchRequest("binary search tree", workspaceId, 10);

        when(workspaceService.getWorkspaceById(userId, workspaceId))
                .thenThrow(new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        assertThrows(ResourceNotFoundException.class, () -> searchService.search(userId, request));

        verify(workspaceService).getWorkspaceById(userId, workspaceId);
        verifyNoInteractions(searchClient);
    }

    @Test
    @DisplayName("search should propagate SearchException when SearchClient encounters engine failure")
    void search_SearchClientThrows_Propagates() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        SearchRequest request = new SearchRequest("binary search tree", workspaceId, 10);

        WorkspaceResponse mockWorkspace = new WorkspaceResponse(
                workspaceId, "CS", "Notes", userId, "Alice", Instant.now(), Instant.now()
        );
        when(workspaceService.getWorkspaceById(userId, workspaceId)).thenReturn(mockWorkspace);

        when(searchClient.executeSearch(any(), any(), any(), any()))
                .thenThrow(new SearchException("AI retrieval engine unavailable."));

        assertThrows(SearchException.class, () -> searchService.search(userId, request));
    }
}
