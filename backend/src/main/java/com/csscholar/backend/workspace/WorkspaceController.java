package com.csscholar.backend.workspace;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workspace created successfully.", workspace));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listWorkspaces(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<WorkspaceResponse> workspaces = workspaceService.listAccessibleWorkspaces(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Workspaces retrieved successfully.", workspaces));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        WorkspaceResponse workspace = workspaceService.getWorkspaceById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Workspace retrieved successfully.", workspace));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        WorkspaceResponse updated = workspaceService.updateWorkspace(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Workspace updated successfully.", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        workspaceService.deleteWorkspace(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Workspace deleted successfully.", null));
    }
}

