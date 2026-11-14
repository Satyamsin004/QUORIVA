package com.csscholar.backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TutorCitationDto {

    @JsonProperty("sourceId")
    @JsonAlias({"source_id", "sourceId"})
    private String sourceId; // e.g. "S1"

    @JsonProperty("chunkId")
    @JsonAlias({"chunk_id", "chunkId"})
    private UUID chunkId;

    @JsonProperty("documentId")
    @JsonAlias({"document_id", "documentId"})
    private UUID documentId;

    @JsonProperty("documentTitle")
    @JsonAlias({"document_title", "documentTitle"})
    private String documentTitle;

    @JsonProperty("pageNumber")
    @JsonAlias({"page_number", "pageNumber"})
    private Integer pageNumber;

    @JsonProperty("slideNumber")
    @JsonAlias({"slide_number", "slideNumber"})
    private Integer slideNumber;

    @JsonProperty("sectionHeading")
    @JsonAlias({"section_heading", "sectionHeading"})
    private String sectionHeading;

    @JsonProperty("excerpt")
    private String excerpt;

    @JsonProperty("relevanceScore")
    @JsonAlias({"relevance_score", "relevanceScore"})
    private Double relevanceScore;

    @JsonProperty("rank")
    private Integer rank;

    public TutorCitationDto() {}

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}

