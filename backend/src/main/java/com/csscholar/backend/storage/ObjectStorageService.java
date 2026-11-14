package com.csscholar.backend.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {

    String generatePresignedUploadUrl(String storageKey, String contentType, long contentLength, Duration expiration);

    String generatePresignedDownloadUrl(String storageKey, Duration expiration, String downloadFilename);

    boolean objectExists(String storageKey);

    StorageObjectMetadata getObjectMetadata(String storageKey);

    InputStream getObjectContent(String storageKey);

    byte[] getObjectBytes(String storageKey, int maxBytes);

    void deleteObject(String storageKey);

    void ensureBucketExists();
}

