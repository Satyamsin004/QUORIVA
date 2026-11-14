package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.LibraryResourceStatus;
import com.csscholar.backend.library.enums.ModerationAction;

import java.time.Instant;
import java.util.UUID;

public class ModerationLogResponse {

    private UUID id;
    private UUID resourceId;
    private String resourceTitle;
    private UUID moderatorId;
    private String moderatorName;
    private ModerationAction action;
    private LibraryResourceStatus previousStatus;
    private LibraryResourceStatus newStatus;
    private String reason;
    private String notes;
    private Instant createdAt;

    public ModerationLogResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public UUID getModeratorId() {
        return moderatorId;
    }

    public void setModeratorId(UUID moderatorId) {
        this.moderatorId = moderatorId;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public void setModeratorName(String moderatorName) {
        this.moderatorName = moderatorName;
    }

    public ModerationAction getAction() {
        return action;
    }

    public void setAction(ModerationAction action) {
        this.action = action;
    }

    public LibraryResourceStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(LibraryResourceStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public LibraryResourceStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(LibraryResourceStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

