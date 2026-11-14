package com.csscholar.backend.chat.dto;

import java.util.UUID;

public class MessageSourceDto {

    private UUID id;
    private UUID chunkId;
    private UUID documentId;
    private String documentTitle;
    private Integer sourceRank;
    private String citationLabel;
    private Double relevanceScore;
    private Integer pageNumber;
    private Integer slideNumber;
    private String sectionHeading;
    private String chunkContent;

    public MessageSourceDto() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getChunkId() {
        return chunkId;
    }

    public void setChunkId(UUID chunkId) {
        this.chunkId = chunkId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public Integer getSourceRank() {
        return sourceRank;
    }

    public void setSourceRank(Integer sourceRank) {
        this.sourceRank = sourceRank;
    }

    public String getCitationLabel() {
        return citationLabel;
    }

    public void setCitationLabel(String citationLabel) {
        this.citationLabel = citationLabel;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getSlideNumber() {
        return slideNumber;
    }

    public void setSlideNumber(Integer slideNumber) {
        this.slideNumber = slideNumber;
    }

    public String getSectionHeading() {
        return sectionHeading;
    }

    public void setSectionHeading(String sectionHeading) {
        this.sectionHeading = sectionHeading;
    }

    public String getChunkContent() {
        return chunkContent;
    }

    public void setChunkContent(String chunkContent) {
        this.chunkContent = chunkContent;
    }
}

