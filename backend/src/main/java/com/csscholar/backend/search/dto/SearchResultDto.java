package com.csscholar.backend.search.dto;

import java.util.UUID;

public class SearchResultDto {

    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private UUID workspaceId;
    private String chunkContent;
    private Integer pageNumber;
    private Integer slideNumber;
    private String sectionHeading;
    private int chunkIndex;
    private String sourceType;
    private int finalRank;
    private double rerankerScore;
    private double rrfScore;
    private Integer denseRank;
    private Integer keywordRank;
    private Double denseSimilarity;
    private Double keywordScore;
    private Integer segmentIndex;

    public SearchResultDto() {}

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

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getChunkContent() {
        return chunkContent;
    }

    public void setChunkContent(String chunkContent) {
        this.chunkContent = chunkContent;
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

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public int getFinalRank() {
        return finalRank;
    }

    public void setFinalRank(int finalRank) {
        this.finalRank = finalRank;
    }

    public double getRerankerScore() {
        return rerankerScore;
    }

    public void setRerankerScore(double rerankerScore) {
        this.rerankerScore = rerankerScore;
    }

    public double getRrfScore() {
        return rrfScore;
    }

    public void setRrfScore(double rrfScore) {
        this.rrfScore = rrfScore;
    }

    public Integer getDenseRank() {
        return denseRank;
    }

    public void setDenseRank(Integer denseRank) {
        this.denseRank = denseRank;
    }

    public Integer getKeywordRank() {
        return keywordRank;
    }

    public void setKeywordRank(Integer keywordRank) {
        this.keywordRank = keywordRank;
    }

    public Double getDenseSimilarity() {
        return denseSimilarity;
    }

    public void setDenseSimilarity(Double denseSimilarity) {
        this.denseSimilarity = denseSimilarity;
    }

    public Double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(Double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public Integer getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(Integer segmentIndex) {
        this.segmentIndex = segmentIndex;
    }
}

