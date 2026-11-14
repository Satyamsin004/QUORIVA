package com.csscholar.backend.document.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.document.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    private FileValidator validator;
    private static final long MAX_BYTES = 52428800L; // 50MB

    @BeforeEach
    void setUp() {
        validator = new FileValidator();
    }

    @Test
    @DisplayName("Should accept valid PDF upload request")
    void shouldAcceptValidPdfRequest() {
        FileType type = validator.validateUploadRequest("research_paper.pdf", "application/pdf", 1024L, MAX_BYTES);
        assertThat(type).isEqualTo(FileType.PDF);
    }

    @Test
    @DisplayName("Should accept valid DOCX upload request")
    void shouldAcceptValidDocxRequest() {
        FileType type = validator.validateUploadRequest(
                "assignment.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                2048L,
                MAX_BYTES
        );
        assertThat(type).isEqualTo(FileType.DOCX);
    }

    @Test
    @DisplayName("Should accept valid PPTX upload request")
    void shouldAcceptValidPptxRequest() {
        FileType type = validator.validateUploadRequest(
                "lecture.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                4096L,
                MAX_BYTES
        );
        assertThat(type).isEqualTo(FileType.PPTX);
    }

    @Test
    @DisplayName("Should reject file size exceeding maximum limit")
    void shouldRejectOversizedFile() {
        assertThatThrownBy(() -> validator.validateUploadRequest(
                "big.pdf", "application/pdf", MAX_BYTES + 1, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("Should reject file size of 0 or negative")
    void shouldRejectZeroOrNegativeSize() {
        assertThatThrownBy(() -> validator.validateUploadRequest(
                "empty.pdf", "application/pdf", 0L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("greater than 0 bytes");

        assertThatThrownBy(() -> validator.validateUploadRequest(
                "negative.pdf", "application/pdf", -5L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should reject path traversal characters in filename")
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> validator.validateUploadRequest(
                "../../secret.pdf", "application/pdf", 1024L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid or dangerous characters");

        assertThatThrownBy(() -> validator.validateUploadRequest(
                "dir/file.pdf", "application/pdf", 1024L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should reject blocked executable extensions")
    void shouldRejectBlockedExtensions() {
        assertThatThrownBy(() -> validator.validateUploadRequest(
                "malware.exe", "application/octet-stream", 1024L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("prohibited");

        assertThatThrownBy(() -> validator.validateUploadRequest(
                "script.sh", "text/x-shellscript", 1024L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("prohibited");
    }

    @Test
    @DisplayName("Should reject mismatched Content-Type")
    void shouldRejectMismatchedContentType() {
        assertThatThrownBy(() -> validator.validateUploadRequest(
                "document.pdf", "application/vnd.ms-excel", 1024L, MAX_BYTES
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("Should validate valid PDF magic bytes")
    void shouldValidatePdfMagicBytes() {
        byte[] validPdfHeader = "%PDF-1.7 header bytes...".getBytes(StandardCharsets.US_ASCII);
        validator.validateFileSignature(FileType.PDF, validPdfHeader);

        byte[] invalidHeader = "GIF89a image".getBytes(StandardCharsets.US_ASCII);
        assertThatThrownBy(() -> validator.validateFileSignature(FileType.PDF, invalidHeader))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid PDF");
    }

    @Test
    @DisplayName("Should validate valid DOCX zip signature and structure")
    void shouldValidateDocxStructure() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write("<Types></Types>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write("<document></document>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        byte[] zipBytes = baos.toByteArray();
        validator.validateFileSignature(FileType.DOCX, zipBytes);
        validator.validateOfficeDocumentStructure(FileType.DOCX, zipBytes);
    }

    @Test
    @DisplayName("Should compute correct SHA-256 hex string")
    void shouldComputeCorrectSha256() {
        byte[] content = "Hello CS Scholar AI".getBytes(StandardCharsets.UTF_8);
        String hash = validator.computeSha256(new ByteArrayInputStream(content));
        assertThat(hash).isNotNull().hasSize(64);
    }
}

