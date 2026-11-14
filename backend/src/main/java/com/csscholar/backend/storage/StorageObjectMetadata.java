package com.csscholar.backend.storage;

import java.time.Instant;

public class StorageObjectMetadata {

    private final long contentLength;
    private final String contentType;
    private final String eTag;
    private final Instant lastModified;

    public StorageObjectMetadata(long contentLength, String contentType, String eTag, Instant lastModified) {
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.eTag = eTag;
        this.lastModified = lastModified;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getETag() {
        return eTag;
    }

    public Instant getLastModified() {
        return lastModified;
    }
}

