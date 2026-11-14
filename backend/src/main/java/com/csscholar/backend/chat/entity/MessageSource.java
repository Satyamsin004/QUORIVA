package com.csscholar.backend.chat.entity;

import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.DocumentChunk;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_sources")
public class MessageSource implements Persistable<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "source_rank", nullable = false)
    private Integer sourceRank = 1;

    @Column(name = "citation_label", nullable = false, length = 50)
    private String citationLabel;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MessageSource() {
    }

    public MessageSource(Message message, DocumentChunk chunk, Document document, Integer sourceRank, String citationLabel, Double relevanceScore) {
        this.message = message;
        this.chunk = chunk;
        this.document = document;
        this.sourceRank = sourceRank != null ? sourceRank : 1;
        this.citationLabel = citationLabel;
        this.relevanceScore = relevanceScore;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.sourceRank == null) {
            this.sourceRank = 1;
        }
    }

    @Transient
    @Override
    public boolean isNew() {
        return this.createdAt == null;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    public void setChunk(DocumentChunk chunk) {
        this.chunk = chunk;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

