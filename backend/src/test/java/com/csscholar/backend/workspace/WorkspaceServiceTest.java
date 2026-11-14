package com.csscholar.backend.workspace;

import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User owner;
    private User otherUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = new User("Alice", "alice@example.com", "hash", Role.STUDENT);
        owner.setId(UUID.randomUUID());

        otherUser = new User("Bob", "bob@example.com", "hash", Role.STUDENT);
        otherUser.setId(UUID.randomUUID());

        workspace = new Workspace("Algorithms 101", "CS Study Group", owner);
        workspace.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Creating workspace should persist workspace and owner membership")
    void shouldCreateWorkspaceSuccessfully() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest("New Workspace", "Description");
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace w = invocation.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });

        WorkspaceResponse response = workspaceService.createWorkspace(owner.getId(), req);

        assertNotNull(response);
        assertEquals("New Workspace", response.getName());
        assertEquals(owner.getId(), response.getOwnerId());
        verify(memberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("Listing workspaces returns accessible workspaces for user")
    void shouldListAccessibleWorkspaces() {
        when(workspaceRepository.findAccessibleWorkspaces(owner.getId())).thenReturn(List.of(workspace));

        List<WorkspaceResponse> list = workspaceService.listAccessibleWorkspaces(owner.getId());

        assertEquals(1, list.size());
        assertEquals("Algorithms 101", list.get(0).getName());
    }

    @Test
    @DisplayName("Getting workspace by ID allows access for owner")
    void shouldAllowOwnerAccess() {
        when(workspaceRepository.findByIdWithOwner(workspace.getId())).thenReturn(Optional.of(workspace));

        WorkspaceResponse response = workspaceService.getWorkspaceById(owner.getId(), workspace.getId());

        assertNotNull(response);
        assertEquals(workspace.getId(), response.getId());
    }

    @Test
    @DisplayName("Getting workspace by ID throws ForbiddenException for unauthorized user")
    void shouldDenyAccessToUnauthorizedUser() {
        when(workspaceRepository.findByIdWithOwner(workspace.getId())).thenReturn(Optional.of(workspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), otherUser.getId())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> workspaceService.getWorkspaceById(otherUser.getId(), workspace.getId()));
    }

    @Test
    @DisplayName("Only owner can delete workspace; other user gets ForbiddenException")
    void shouldOnlyAllowOwnerToDeleteWorkspace() {
        when(workspaceRepository.findByIdWithOwner(workspace.getId())).thenReturn(Optional.of(workspace));

        assertThrows(ForbiddenException.class, () -> workspaceService.deleteWorkspace(otherUser.getId(), workspace.getId()));
        verify(workspaceRepository, never()).delete(any(Workspace.class));

        // When owner deletes
        workspaceService.deleteWorkspace(owner.getId(), workspace.getId());
        verify(workspaceRepository, times(1)).delete(workspace);
    }
}

