package com.csscholar.backend.document.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.document.dto.*;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.entity.IngestionOutbox;
import com.csscholar.backend.document.enums.DocumentVisibility;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.enums.ProcessingStatus;
import com.csscholar.backend.document.enums.ScanStatus;
import com.csscholar.backend.document.event.DocumentUploadedEvent;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.document.repository.IngestionOutboxRepository;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.storage.StorageObjectMetadata;
import com.csscholar.backend.storage.StorageProperties;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.MemberRole;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceMember;
import com.csscholar.backend.workspace.WorkspaceMemberRepository;
import com.csscholar.backend.workspace.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService storageService;
    private final StorageProperties storageProperties;
    private final FileValidator fileValidator;
    private final StorageKeyGenerator storageKeyGenerator;
    private final IngestionOutboxRepository ingestionOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final MalwareScanner malwareScanner;

    private final IngestionClient ingestionClient;

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           UserRepository userRepository,
                           ObjectStorageService storageService,
                           StorageProperties storageProperties,
                           FileValidator fileValidator,
                           StorageKeyGenerator storageKeyGenerator,
                           IngestionOutboxRepository ingestionOutboxRepository,
                           ApplicationEventPublisher eventPublisher,
                           ObjectMapper objectMapper,
                           MalwareScanner malwareScanner,
                           @Autowired(required = false) IngestionClient ingestionClient) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.fileValidator = fileValidator;
        this.storageKeyGenerator = storageKeyGenerator;
        this.ingestionOutboxRepository = ingestionOutboxRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.malwareScanner = malwareScanner != null ? malwareScanner : new HeuristicMalwareScanner();
        this.ingestionClient = ingestionClient;
    }

    public DocumentService(DocumentRepository documentRepository,
                           WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           UserRepository userRepository,
                           ObjectStorageService storageService,
                           StorageProperties storageProperties,
                           FileValidator fileValidator,
                           StorageKeyGenerator storageKeyGenerator) {
        this(documentRepository, workspaceRepository, workspaceMemberRepository, userRepository,
             storageService, storageProperties, fileValidator, storageKeyGenerator,
             null, null, new ObjectMapper(), new HeuristicMalwareScanner(), null);
    }

    @Transactional
    public UploadUrlResponse requestUploadUrl(UUID userId, UploadUrlRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Workspace workspace = workspaceRepository.findByIdWithOwner(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + request.getWorkspaceId()));

        validateWorkspaceUploadPermission(workspace, userId);

        FileType fileType = fileValidator.validateUploadRequest(
                request.getFilename(),
                request.getContentType(),
                request.getFileSize(),
                storageProperties.getMaxFileSizeBytes()
        );

        UUID documentId = UUID.randomUUID();
        String storageKey = storageKeyGenerator.generateStorageKey(workspace.getId(), documentId, request.getFilename());

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            String orig = request.getFilename().trim();
            int dotIdx = orig.lastIndexOf('.');
            title = dotIdx > 0 ? orig.substring(0, dotIdx) : orig;
        }

        Document document = new Document(
                user,
                workspace,
                title,
                request.getFilename().trim(),
                storageKey,
                fileType,
                fileType.getMimeType(),
                request.getFileSize(),
                request.getChecksumSha256(),
                request.getDescription()
        );
        document.setId(documentId);
        document.setScanStatus(ScanStatus.PENDING);
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            document.setSubject(request.getSubject().trim());
        }
        Document saved = documentRepository.save(document);

        String presignedUrl = storageService.generatePresignedUploadUrl(
                storageKey,
                fileType.getMimeType(),
                request.getFileSize(),
                storageProperties.getPresignedUrlDuration()
        );

        Map<String, String> requiredHeaders = new HashMap<>();
        requiredHeaders.put("Content-Type", fileType.getMimeType());
        requiredHeaders.put("Content-Length", String.valueOf(request.getFileSize()));

        return new UploadUrlResponse(
                saved.getId(),
                presignedUrl,
                storageKey,
                storageProperties.getPresignedUrlExpirationSeconds(),
                requiredHeaders
        );
    }

    @Transactional
    public DocumentResponse completeUpload(UUID userId, UUID documentId, CompleteUploadRequest request) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceAccess(document.getWorkspace(), userId);

        if (document.getProcessingStatus() == ProcessingStatus.UPLOADED
                || document.getProcessingStatus() == ProcessingStatus.PENDING_PROCESSING
                || document.getProcessingStatus() == ProcessingStatus.PROCESSED) {
            return DocumentResponse.fromEntity(document);
        }

        if (document.getProcessingStatus() != ProcessingStatus.PENDING_UPLOAD) {
            throw new BadRequestException("Document is not in PENDING_UPLOAD status. Current status: " + document.getProcessingStatus());
        }

        if (!storageService.objectExists(document.getStorageKey())) {
            throw new BadRequestException("Uploaded object does not exist in storage for key: " + document.getStorageKey());
        }

        StorageObjectMetadata metadata = storageService.getObjectMetadata(document.getStorageKey());
        long actualSize = metadata.getContentLength();
        if (actualSize <= 0) {
            throw new BadRequestException("Uploaded file is empty (0 bytes).");
        }
        if (actualSize > storageProperties.getMaxFileSizeBytes()) {
            storageService.deleteObject(document.getStorageKey());
            document.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(document);
            throw new BadRequestException("Uploaded file size (" + actualSize + " bytes) exceeds maximum limit of " +
                    (storageProperties.getMaxFileSizeBytes() / (1024 * 1024)) + " MB.");
        }

        byte[] headerBytes = storageService.getObjectBytes(document.getStorageKey(), 32);
        fileValidator.validateFileSignature(document.getFileType(), headerBytes);

        // Malware and malicious exploit scanning
        if (malwareScanner != null) {
            try (InputStream is = storageService.getObjectContent(document.getStorageKey())) {
                ScanResult scanResult = malwareScanner.scan(is, document.getOriginalFilename());
                document.setScanStatus(scanResult.status());
                if (!scanResult.isAcceptable()) {
                    log.error("Malware scan rejected upload for document {}: status={}, threat={}, details={}",
                            document.getId(), scanResult.status(), scanResult.threatName(), scanResult.details());
                    try {
                        storageService.deleteObject(document.getStorageKey());
                    } catch (Exception ex) {
                        log.error("Failed to delete rejected object from storage {}: {}", document.getStorageKey(), ex.getMessage());
                    }
                    document.setProcessingStatus(ProcessingStatus.FAILED);
                    documentRepository.saveAndFlush(document);
                    throw new BadRequestException("Malware or security policy violation detected: " +
                            (scanResult.details() != null ? scanResult.details() : scanResult.threatName()));
                }
            } catch (BadRequestException bre) {
                throw bre;
            } catch (Exception e) {
                log.error("Failed to perform security inspection on document {}: {}", document.getId(), e.getMessage());
                document.setScanStatus(ScanStatus.SCAN_FAILED);
                try {
                    storageService.deleteObject(document.getStorageKey());
                } catch (Exception ex) {
                    log.error("Failed to delete object from storage after scan failure {}: {}", document.getStorageKey(), ex.getMessage());
                }
                document.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.saveAndFlush(document);
                throw new BadRequestException("Security inspection failed for document: " + e.getMessage());
            }
        } else {
            document.setScanStatus(ScanStatus.CLEAN);
        }

        if (document.getFileType() == FileType.DOCX || document.getFileType() == FileType.PPTX) {
            try (InputStream is = storageService.getObjectContent(document.getStorageKey())) {
                byte[] contentBytes = is.readNBytes(2 * 1024 * 1024);
                fileValidator.validateOfficeDocumentStructure(document.getFileType(), contentBytes);
            } catch (IOException e) {
                throw new BadRequestException("Failed to read document for structure validation: " + e.getMessage());
            }
        }

        String calculatedHash;
        try (InputStream is = storageService.getObjectContent(document.getStorageKey())) {
            calculatedHash = fileValidator.computeSha256(is);
        } catch (IOException e) {
            throw new BadRequestException("Failed to stream document for checksum verification: " + e.getMessage());
        }

        if (document.getChecksumSha256() != null && !document.getChecksumSha256().isBlank()) {
            if (!document.getChecksumSha256().equalsIgnoreCase(calculatedHash)) {
                throw new BadRequestException("Checksum verification failed. Expected: " +
                        document.getChecksumSha256() + ", calculated: " + calculatedHash);
            }
        }

        if (request != null && request.getChecksumSha256() != null && !request.getChecksumSha256().isBlank()) {
            if (!request.getChecksumSha256().equalsIgnoreCase(calculatedHash)) {
                throw new BadRequestException("Supplied checksum verification failed. Expected: " +
                        request.getChecksumSha256() + ", calculated: " + calculatedHash);
            }
        }

        document.setFileSize(actualSize);
        document.setChecksumSha256(calculatedHash);
        document.setProcessingStatus(ProcessingStatus.PENDING_PROCESSING);
        document.setUploadedAt(Instant.now());

        Document saved = documentRepository.save(document);
        log.info("Document upload completed successfully: id={}, key={}", saved.getId(), saved.getStorageKey());

        // Transactional Outbox Event for Ingestion Engine
        if (ingestionOutboxRepository != null) {
            String payload = buildIngestionPayload(saved);
            IngestionOutbox outbox = new IngestionOutbox(saved, "INGESTION_REQUESTED", payload);
            IngestionOutbox savedOutbox = ingestionOutboxRepository.save(outbox);
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new DocumentUploadedEvent(saved.getId(), savedOutbox.getId()));
            }
        }

        return DocumentResponse.fromEntity(saved);
    }

    private String buildIngestionPayload(Document doc) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("document_id", doc.getId().toString());
            map.put("workspace_id", doc.getWorkspace().getId().toString());
            map.put("storage_key", doc.getStorageKey());
            map.put("file_type", doc.getFileType().name());
            map.put("mime_type", doc.getMimeType());
            map.put("file_size", doc.getFileSize());
            map.put("processing_version", 1);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize ingestion payload for document {}: {}", doc.getId(), e.getMessage());
            return String.format(
                    "{\"document_id\":\"%s\",\"workspace_id\":\"%s\",\"storage_key\":\"%s\",\"file_type\":\"%s\",\"mime_type\":\"%s\",\"file_size\":%d,\"processing_version\":1}",
                    doc.getId(), doc.getWorkspace().getId(), doc.getStorageKey(), doc.getFileType().name(), doc.getMimeType(), doc.getFileSize()
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(UUID userId, UUID workspaceId, String query,
                                                FileType fileType, ProcessingStatus status, Pageable pageable) {
        List<Workspace> accessibleWorkspaces = workspaceRepository.findAccessibleWorkspaces(userId);
        if (accessibleWorkspaces.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> accessibleIds = accessibleWorkspaces.stream().map(Workspace::getId).toList();

        if (workspaceId != null) {
            if (!accessibleIds.contains(workspaceId)) {
                throw new ForbiddenException("You do not have access to workspace: " + workspaceId);
            }
        }

        Specification<Document> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (workspaceId != null) {
                predicates.add(cb.equal(root.get("workspace").get("id"), workspaceId));
            } else {
                predicates.add(root.get("workspace").get("id").in(accessibleIds));
            }

            if (query != null && !query.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + query.trim().toLowerCase() + "%"));
            }

            if (fileType != null) {
                predicates.add(cb.equal(root.get("fileType"), fileType));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("processingStatus"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return documentRepository.findAll(spec, pageable).map(DocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID userId, UUID documentId) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceAccess(document.getWorkspace(), userId);
        return DocumentResponse.fromEntity(document);
    }

    @Transactional(readOnly = true)
    public byte[] getDocumentPagePreview(UUID userId, UUID documentId, int pageNumber) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceAccess(document.getWorkspace(), userId);
        if (ingestionClient == null) {
            return null;
        }
        return ingestionClient.fetchPagePreview(document.getStorageKey(), pageNumber);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse generateDownloadUrl(UUID userId, UUID documentId) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceAccess(document.getWorkspace(), userId);

        if (document.getScanStatus() != ScanStatus.CLEAN) {
            throw new BadRequestException("Document cannot be downloaded because security scan status is: " + document.getScanStatus());
        }

        if (document.getProcessingStatus() != ProcessingStatus.UPLOADED &&
                document.getProcessingStatus() != ProcessingStatus.PENDING_PROCESSING &&
                document.getProcessingStatus() != ProcessingStatus.PROCESSING &&
                document.getProcessingStatus() != ProcessingStatus.PROCESSED) {
            throw new BadRequestException("Document is not available for download. Current status: " + document.getProcessingStatus());
        }

        String downloadUrl = storageService.generatePresignedDownloadUrl(
                document.getStorageKey(),
                storageProperties.getPresignedUrlDuration(),
                document.getOriginalFilename()
        );

        return new DownloadUrlResponse(documentId, downloadUrl, storageProperties.getPresignedUrlExpirationSeconds());
    }

    @Transactional(readOnly = true)
    public DocumentStatusResponse getDocumentStatus(UUID userId, UUID documentId) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceAccess(document.getWorkspace(), userId);
        return new DocumentStatusResponse(
                document.getId(),
                document.getProcessingStatus(),
                document.getEmbeddingStatus(),
                document.getPageCount(),
                null,
                document.getUploadedAt(),
                document.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findActiveByIdWithDetails(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        validateWorkspaceDeletePermission(document, userId);

        document.setDeletedAt(Instant.now());
        document.setProcessingStatus(ProcessingStatus.DELETED);
        documentRepository.save(document);

        try {
            storageService.deleteObject(document.getStorageKey());
        } catch (Exception e) {
            log.warn("Storage object deletion failed or deferred for key: {}. Message: {}", document.getStorageKey(), e.getMessage());
        }
    }

    private void validateWorkspaceAccess(Workspace workspace, UUID userId) {
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), userId);
        if (!isMember) {
            throw new ForbiddenException("You do not have permission to access documents in this workspace.");
        }
    }

    private void validateWorkspaceUploadPermission(Workspace workspace, UUID userId) {
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }
        Optional<WorkspaceMember> memberOpt = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId);
        if (memberOpt.isEmpty()) {
            throw new ForbiddenException("You do not have permission to upload documents to this workspace.");
        }
        WorkspaceMember member = memberOpt.get();
        if (member.getMemberRole() == MemberRole.VIEWER) {
            throw new ForbiddenException("Viewer role does not have permission to upload documents.");
        }
    }

    private void validateWorkspaceDeletePermission(Document document, UUID userId) {
        if (document.getOwner().getId().equals(userId)) {
            return;
        }

        Workspace workspace = document.getWorkspace();
        if (workspace.getOwner().getId().equals(userId)) {
            return;
        }

        Optional<WorkspaceMember> memberOpt = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId);
        if (memberOpt.isEmpty()) {
            throw new ForbiddenException("You do not have permission to delete this document.");
        }

        WorkspaceMember member = memberOpt.get();
        if (member.getMemberRole() == MemberRole.VIEWER) {
            throw new ForbiddenException("Viewer role cannot delete documents.");
        }
    }
}
