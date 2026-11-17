package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class TargetMaterialSummaryDto {

    @JsonProperty("documentId")
    private UUID documentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("startPage")
    private Integer startPage;

    @JsonProperty("endPage")
    private Integer endPage;

    @JsonProperty("pageCount")
    private Integer pageCount;

    public TargetMaterialSummaryDto() {}

    public TargetMaterialSummaryDto(UUID documentId, String title, Integer startPage, Integer endPage, Integer pageCount) {
        this.documentId = documentId;
        this.title = title;
        this.startPage = startPage;
        this.endPage = endPage;
        this.pageCount = pageCount;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStartPage() {
        return startPage;
    }

    public void setStartPage(Integer startPage) {
        this.startPage = startPage;
    }

    public Integer getEndPage() {
        return endPage;
    }

    public void setEndPage(Integer endPage) {
        this.endPage = endPage;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}
