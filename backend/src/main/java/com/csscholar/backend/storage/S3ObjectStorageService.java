package com.csscholar.backend.storage;

import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.common.StorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Service
public class S3ObjectStorageService implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public S3ObjectStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            ensureBucketExists();
        } catch (Exception e) {
            log.warn("S3 bucket initialization deferred: {}", e.getMessage());
        }
    }

    @Override
    public void ensureBucketExists() {
        String bucket = properties.getBucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket '{}' already exists and is accessible.", bucket);
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket '{}' does not exist. Creating bucket...", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket '{}' created successfully.", bucket);
        } catch (AwsServiceException e) {
            if (e.statusCode() == 404) {
                log.info("S3 bucket '{}' returned 404. Creating bucket...", bucket);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("S3 bucket '{}' created successfully.", bucket);
            } else {
                log.warn("S3 bucket check received status code {}: {}", e.statusCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Could not check/create bucket '{}': {}", bucket, e.getMessage());
        }
    }

    @Override
    public String generatePresignedUploadUrl(String storageKey, String contentType, long contentLength, Duration expiration) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiration != null ? expiration : properties.getPresignedUrlDuration())
                    .putObjectRequest(putRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for key: {}", storageKey, e);
            throw new StorageException("Failed to generate presigned upload URL", e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String storageKey, Duration expiration, String downloadFilename) {
        try {
            GetObjectRequest.Builder getRequestBuilder = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey);

            if (downloadFilename != null && !downloadFilename.isBlank()) {
                String sanitized = downloadFilename.replace("\"", "").replace("\r", "").replace("\n", "");
                getRequestBuilder.responseContentDisposition("attachment; filename=\"" + sanitized + "\"");
            }

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration != null ? expiration : properties.getPresignedUrlDuration())
                    .getObjectRequest(getRequestBuilder.build())
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL for key: {}", storageKey, e);
            throw new StorageException("Failed to generate presigned download URL", e);
        }
    }

    @Override
    public boolean objectExists(String storageKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (AwsServiceException e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("Error checking object existence for key: " + storageKey, e);
        } catch (Exception e) {
            throw new StorageException("Error communicating with object storage", e);
        }
    }

    @Override
    public StorageObjectMetadata getObjectMetadata(String storageKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
            return new StorageObjectMetadata(
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified()
            );
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Storage object not found: " + storageKey);
        } catch (AwsServiceException e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Storage object not found: " + storageKey);
            }
            throw new StorageException("Error fetching metadata for key: " + storageKey, e);
        } catch (Exception e) {
            throw new StorageException("Error communicating with object storage", e);
        }
    }

    @Override
    public InputStream getObjectContent(String storageKey) {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Storage object not found: " + storageKey);
        } catch (AwsServiceException e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Storage object not found: " + storageKey);
            }
            throw new StorageException("Error downloading object content for key: " + storageKey, e);
        } catch (Exception e) {
            throw new StorageException("Error communicating with object storage", e);
        }
    }

    @Override
    public byte[] getObjectBytes(String storageKey, int maxBytes) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .range("bytes=0-" + (maxBytes - 1))
                    .build();

            try (ResponseInputStream<GetObjectResponse> is = s3Client.getObject(request)) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.debug("Range request failed, falling back to streaming read: {}", e.getMessage());
            try (InputStream is = getObjectContent(storageKey)) {
                return is.readNBytes(maxBytes);
            } catch (Exception ex) {
                throw new StorageException("Failed to read bytes for key: " + storageKey, ex);
            }
        }
    }

    @Override
    public void deleteObject(String storageKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build());
            log.info("Object deleted from storage: {}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete object from storage: {}", storageKey, e);
            throw new StorageException("Failed to delete object from storage: " + storageKey, e);
        }
    }
}

