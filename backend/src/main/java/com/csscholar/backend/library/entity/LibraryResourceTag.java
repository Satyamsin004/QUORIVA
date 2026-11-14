package com.csscholar.backend.library.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "library_resource_tags")
public class LibraryResourceTag {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private LibraryResource resource;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LibraryResourceTag() {
    }

    public LibraryResourceTag(LibraryResource resource, String tag) {
        this.resource = resource;
        this.tag = tag;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LibraryResource getResource() {
        return resource;
    }

    public void setResource(LibraryResource resource) {
        this.resource = resource;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryResourceTag that = (LibraryResourceTag) o;
        return Objects.equals(tag != null ? tag.toLowerCase() : null, that.tag != null ? that.tag.toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag != null ? tag.toLowerCase() : null);
    }
}

