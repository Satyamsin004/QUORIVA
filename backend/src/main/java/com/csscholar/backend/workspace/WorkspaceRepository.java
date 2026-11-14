package com.csscholar.backend.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT DISTINCT w FROM Workspace w LEFT JOIN w.members m WHERE w.owner.id = :userId OR m.user.id = :userId ORDER BY w.updatedAt DESC")
    List<Workspace> findAccessibleWorkspaces(@Param("userId") UUID userId);

    List<Workspace> findByOwnerId(UUID ownerId);

    @Query("SELECT w FROM Workspace w LEFT JOIN FETCH w.owner WHERE w.id = :id")
    Optional<Workspace> findByIdWithOwner(@Param("id") UUID id);
}

