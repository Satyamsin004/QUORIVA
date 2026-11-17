package com.csscholar.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetMaterialRequest {

    @JsonProperty("documentId")
    @JsonAlias({"document_id", "id"})
    private UUID documentId;

    @JsonProperty("startPage")
    @JsonAlias({"start_page", "fromPage", "from_page"})
    private Integer startPage;

    @JsonProperty("endPage")
    @JsonAlias({"end_page", "toPage", "to_page"})
    private Integer endPage;

    public TargetMaterialRequest() {}

    public TargetMaterialRequest(UUID documentId, Integer startPage, Integer endPage) {
        this.documentId = documentId;
        this.startPage = startPage;
        this.endPage = endPage;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
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
}
