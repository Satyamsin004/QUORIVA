package com.csscholar.backend.workspace;

import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(UUID userId, CreateWorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Workspace workspace = new Workspace(request.getName().trim(), request.getDescription(), user);
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember(savedWorkspace, user, MemberRole.OWNER);
        memberRepository.save(ownerMember);

        return WorkspaceResponse.fromEntity(savedWorkspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listAccessibleWorkspaces(UUID userId) {
        return workspaceRepository.findAccessibleWorkspaces(userId)
                .stream()
                .map(WorkspaceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceById(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        validateAccess(workspace, userId);
        return WorkspaceResponse.fromEntity(workspace);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(UUID userId, UUID workspaceId, UpdateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        validateWorkspaceEditorOrOwner(workspace, userId);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workspace.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        Workspace updated = workspaceRepository.save(workspace);
        return WorkspaceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteWorkspace(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        if (!workspace.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the workspace owner can delete this workspace.");
        }

        workspaceRepository.delete(workspace);
    }

    private void validateAccess(Workspace workspace, UUID userId) {
        boolean isOwner = workspace.getOwner().getId().equals(userId);
        if (isOwner) {
            return;
        }

        boolean isMember = memberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), userId);
        if (!isMember) {
            throw new ForbiddenException("You do not have permission to access this workspace.");
        }
    }

    private void validateWorkspaceEditorOrOwner(Workspace workspace, UUID userId) {
        boolean isOwner = workspace.getOwner().getId().equals(userId);
        if (isOwner) {
            return;
        }

        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .orElseThrow(() -> new ForbiddenException("You do not have permission to modify this workspace."));

        if (member.getMemberRole() != MemberRole.OWNER && member.getMemberRole() != MemberRole.EDITOR) {
            throw new ForbiddenException("Only workspace owners and editors can modify workspace details.");
        }
    }
}

