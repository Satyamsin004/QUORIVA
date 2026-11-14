package com.csscholar.backend.document.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.ResourceNotFoundException;
import com.csscholar.backend.document.dto.CompleteUploadRequest;
import com.csscholar.backend.document.dto.DocumentResponse;
import com.csscholar.backend.document.dto.DownloadUrlResponse;
import com.csscholar.backend.document.dto.UploadUrlRequest;
import com.csscholar.backend.document.dto.UploadUrlResponse;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.enums.ProcessingStatus;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.storage.StorageObjectMetadata;
import com.csscholar.backend.storage.StorageProperties;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.MemberRole;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceMember;
import com.csscholar.backend.workspace.WorkspaceMemberRepository;
import com.csscholar.backend.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectStorageService storageService;

    private StorageProperties storageProperties;
    private FileValidator fileValidator;
    private StorageKeyGenerator storageKeyGenerator;
    private DocumentService documentService;

    private User owner;
    private User memberUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setBucket("test-bucket");
        storageProperties.setMaxFileSizeBytes(52428800L);
        storageProperties.setPresignedUrlExpirationSeconds(900L);

        fileValidator = new FileValidator();
        storageKeyGenerator = new StorageKeyGenerator();

        documentService = new DocumentService(
                documentRepository,
                workspaceRepository,
                workspaceMemberRepository,
                userRepository,
                storageService,
                storageProperties,
                fileValidator,
                storageKeyGenerator
        );

        owner = new User("John Owner", "owner@example.com", "hash", Role.STUDENT);
        owner.setId(UUID.randomUUID());

        memberUser = new User("Jane Member", "member@example.com", "hash", Role.STUDENT);
        memberUser.setId(UUID.randomUUID());

        workspace = new Workspace("CS101 Workspace", "Study workspace", owner);
        workspace.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should generate presigned upload URL for valid document request")
    void requestUploadUrl_Success() {
        UploadUrlRequest request = new UploadUrlRequest(
                workspace.getId(),
                "algorithms.pdf",
                "application/pdf",
                1024L,
                null,
                "Algorithms Notes",
                "Chapter 1 notes"
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(workspaceRepository.findByIdWithOwner(workspace.getId())).thenReturn(Optional.of(workspace));
        when(storageService.generatePresignedUploadUrl(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn("https://s3.example.com/presigned-upload");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UploadUrlResponse response = documentService.requestUploadUrl(owner.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isNotNull();
        assertThat(response.getUploadUrl()).isEqualTo("https://s3.example.com/presigned-upload");
        assertThat(response.getStorageKey()).contains("algorithms.pdf");
        assertThat(response.getRequiredHeaders()).containsEntry("Content-Type", "application/pdf");

        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("Should reject upload URL request when user has VIEWER role in workspace")
    void requestUploadUrl_ViewerForbidden() {
        UploadUrlRequest request = new UploadUrlRequest(
                workspace.getId(),
                "slides.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                2048L,
                null,
                null,
                null
        );

        when(userRepository.findById(memberUser.getId())).thenReturn(Optional.of(memberUser));
        when(workspaceRepository.findByIdWithOwner(workspace.getId())).thenReturn(Optional.of(workspace));

        WorkspaceMember viewerMember = new WorkspaceMember(workspace, memberUser, MemberRole.VIEWER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), memberUser.getId()))
                .thenReturn(Optional.of(viewerMember));

        assertThatThrownBy(() -> documentService.requestUploadUrl(memberUser.getId(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Viewer role does not have permission");
    }

    @Test
    @DisplayName("Should complete upload successfully when object exists and signature passes")
    void completeUpload_Success() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);

        byte[] pdfContent = "%PDF-1.4 test content".getBytes(StandardCharsets.US_ASCII);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        when(storageService.objectExists(document.getStorageKey())).thenReturn(true);
        when(storageService.getObjectMetadata(document.getStorageKey())).thenReturn(
                new StorageObjectMetadata(pdfContent.length, "application/pdf", "etag123", Instant.now())
        );
        when(storageService.getObjectBytes(eq(document.getStorageKey()), anyInt()))
                .thenReturn(pdfContent);
        when(storageService.getObjectContent(document.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(pdfContent));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.completeUpload(owner.getId(), docId, new CompleteUploadRequest());

        assertThat(response.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING_PROCESSING);
        assertThat(response.getUploadedAt()).isNotNull();
        assertThat(response.getChecksumSha256()).isNotNull().hasSize(64);
    }

    @Test
    @DisplayName("Should reject complete upload if object does not exist in storage")
    void completeUpload_ObjectNotFound_ThrowsBadRequest() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        when(storageService.objectExists(document.getStorageKey())).thenReturn(false);

        assertThatThrownBy(() -> documentService.completeUpload(owner.getId(), docId, new CompleteUploadRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not exist in storage");
    }

    @Test
    @DisplayName("Should reject complete upload if checksum verification fails")
    void completeUpload_ChecksumMismatch_ThrowsBadRequest() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                "expected-sha256-that-will-not-match",
                null
        );
        document.setId(docId);

        byte[] pdfContent = "%PDF-1.4 test content".getBytes(StandardCharsets.US_ASCII);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        when(storageService.objectExists(document.getStorageKey())).thenReturn(true);
        when(storageService.getObjectMetadata(document.getStorageKey())).thenReturn(
                new StorageObjectMetadata(pdfContent.length, "application/pdf", "etag123", Instant.now())
        );
        when(storageService.getObjectBytes(eq(document.getStorageKey()), anyInt())).thenReturn(pdfContent);
        when(storageService.getObjectContent(document.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(pdfContent));

        assertThatThrownBy(() -> documentService.completeUpload(owner.getId(), docId, new CompleteUploadRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Checksum verification failed");
    }

    @Test
    @DisplayName("Should be idempotent when completing upload on already UPLOADED document")
    void completeUpload_Idempotent() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);
        document.setProcessingStatus(ProcessingStatus.UPLOADED);
        document.setUploadedAt(Instant.now());

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));

        DocumentResponse response = documentService.completeUpload(owner.getId(), docId, new CompleteUploadRequest());
        assertThat(response.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
        verify(storageService, never()).objectExists(anyString());
    }

    @Test
    @DisplayName("Should generate download URL for UPLOADED document")
    void generateDownloadUrl_Success() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);
        document.setProcessingStatus(ProcessingStatus.UPLOADED);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        when(storageService.generatePresignedDownloadUrl(eq(document.getStorageKey()), any(Duration.class), eq(document.getOriginalFilename())))
                .thenReturn("https://s3.example.com/presigned-download");

        DownloadUrlResponse response = documentService.generateDownloadUrl(owner.getId(), docId);

        assertThat(response.getDownloadUrl()).isEqualTo("https://s3.example.com/presigned-download");
        assertThat(response.getDocumentId()).isEqualTo(docId);
    }

    @Test
    @DisplayName("Should reject download URL generation for PENDING_UPLOAD document")
    void generateDownloadUrl_PendingUpload_ThrowsBadRequest() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);
        document.setProcessingStatus(ProcessingStatus.PENDING_UPLOAD);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.generateDownloadUrl(owner.getId(), docId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available for download");
    }

    @Test
    @DisplayName("Should soft delete document and invoke storage deletion")
    void deleteDocument_Success() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.deleteDocument(owner.getId(), docId);

        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.DELETED);
        assertThat(document.getDeletedAt()).isNotNull();
        verify(storageService, times(1)).deleteObject(document.getStorageKey());
    }

    @Test
    @DisplayName("Should reject delete document if user is unauthorized viewer")
    void deleteDocument_ViewerForbidden() {
        UUID docId = UUID.randomUUID();
        Document document = new Document(
                owner,
                workspace,
                "Intro to OS",
                "intro.pdf",
                "workspaces/ws/documents/doc/intro.pdf",
                FileType.PDF,
                "application/pdf",
                1024L,
                null,
                null
        );
        document.setId(docId);

        when(documentRepository.findActiveByIdWithDetails(docId)).thenReturn(Optional.of(document));
        WorkspaceMember viewerMember = new WorkspaceMember(workspace, memberUser, MemberRole.VIEWER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), memberUser.getId()))
                .thenReturn(Optional.of(viewerMember));

        assertThatThrownBy(() -> documentService.deleteDocument(memberUser.getId(), docId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Viewer role cannot delete documents");
    }
}

