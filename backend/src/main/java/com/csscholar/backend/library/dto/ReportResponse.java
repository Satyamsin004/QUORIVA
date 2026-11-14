package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.ReportReasonCategory;
import com.csscholar.backend.library.enums.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public class ReportResponse {

    private UUID id;
    private UUID resourceId;
    private String resourceTitle;
    private UUID reporterId;
    private String reporterName;
    private ReportReasonCategory reasonCategory;
    private String reasonCategoryDisplayName;
    private String explanation;
    private ReportStatus status;
    private String resolutionNotes;
    private UUID resolvedById;
    private String resolvedByName;
    private Instant createdAt;
    private Instant resolvedAt;

    public ReportResponse() {
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

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public ReportReasonCategory getReasonCategory() {
        return reasonCategory;
    }

    public void setReasonCategory(ReportReasonCategory reasonCategory) {
        this.reasonCategory = reasonCategory;
    }

    public String getReasonCategoryDisplayName() {
        return reasonCategoryDisplayName;
    }

    public void setReasonCategoryDisplayName(String reasonCategoryDisplayName) {
        this.reasonCategoryDisplayName = reasonCategoryDisplayName;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public UUID getResolvedById() {
        return resolvedById;
    }

    public void setResolvedById(UUID resolvedById) {
        this.resolvedById = resolvedById;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

