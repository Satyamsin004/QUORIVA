package com.csscholar.backend.document.controller;

import com.csscholar.backend.auth.UserPrincipal;
import com.csscholar.backend.common.ApiResponse;
import com.csscholar.backend.document.dto.*;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.enums.ProcessingStatus;
import com.csscholar.backend.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> requestUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UploadUrlRequest request) {
        UploadUrlResponse response = documentService.requestUploadUrl(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Presigned upload URL generated successfully.", response));
    }

    @PostMapping("/{id}/complete-upload")
    public ResponseEntity<ApiResponse<DocumentResponse>> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CompleteUploadRequest request) {
        DocumentResponse response = documentService.completeUpload(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Document upload verified and completed successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> listDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "workspaceId", required = false) UUID workspaceId,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "fileType", required = false) FileType fileType,
            @RequestParam(value = "status", required = false) ProcessingStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortProperty = sortParts[0].trim();
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        // sanitize sort property to avoid invalid column names
        if (!sortProperty.equals("createdAt") && !sortProperty.equals("updatedAt") &&
                !sortProperty.equals("title") && !sortProperty.equals("fileSize")) {
            sortProperty = "createdAt";
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortProperty));
        Page<DocumentResponse> documents = documentService.listDocuments(principal.getId(), workspaceId, query, fileType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully.", documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        DocumentResponse document = documentService.getDocumentById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Document details retrieved successfully.", document));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        DownloadUrlResponse response = documentService.generateDownloadUrl(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Presigned download URL generated successfully.", response));
    }

    @GetMapping("/{id}/pages/{pageNumber}/preview")
    public ResponseEntity<byte[]> getPagePreview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id,
            @PathVariable("pageNumber") int pageNumber) {
        byte[] imageBytes = documentService.getDocumentPagePreview(principal.getId(), id, pageNumber);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DocumentStatusResponse>> getDocumentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        DocumentStatusResponse response = documentService.getDocumentStatus(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Document status retrieved successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID id) {
        documentService.deleteDocument(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully.", null));
    }
}

