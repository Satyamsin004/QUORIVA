package com.csscholar.backend.document.service;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.document.enums.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FileValidator {

    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] ZIP_MAGIC = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK\x03\x04

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "dll", "bat", "cmd", "sh", "bin", "jar", "war", "py", "js", "ts",
            "jsp", "php", "asp", "aspx", "cgi", "pl", "com", "vbs", "ps1", "elf"
    );

    public FileType validateUploadRequest(String filename, String claimedContentType, long fileSize, long maxAllowedBytes) {
        if (fileSize <= 0) {
            throw new BadRequestException("File size must be greater than 0 bytes.");
        }

        if (fileSize > maxAllowedBytes) {
            throw new BadRequestException("File size exceeds maximum allowed limit of " + (maxAllowedBytes / (1024 * 1024)) + " MB.");
        }

        if (filename == null || filename.trim().isEmpty()) {
            throw new BadRequestException("Filename cannot be empty.");
        }

        String trimmedFilename = filename.trim();

        if (trimmedFilename.length() > 255) {
            throw new BadRequestException("Filename length cannot exceed 255 characters.");
        }

        if (trimmedFilename.contains("..") || trimmedFilename.contains("/") || trimmedFilename.contains("\\") || trimmedFilename.contains("\0")) {
            throw new BadRequestException("Filename contains invalid or dangerous characters.");
        }

        int lastDotIndex = trimmedFilename.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == trimmedFilename.length() - 1) {
            throw new BadRequestException("Filename must contain a valid file extension.");
        }

        String rawExt = trimmedFilename.substring(lastDotIndex + 1).toLowerCase();
        if (BLOCKED_EXTENSIONS.contains(rawExt)) {
            throw new BadRequestException("Uploading executable or script files is strictly prohibited.");
        }

        FileType fileType = FileType.fromFilename(trimmedFilename)
                .orElseThrow(() -> new BadRequestException(
                        "Unsupported file format. Supported formats are: PDF (.pdf), DOCX (.docx), PPTX (.pptx)."
                ));

        if (claimedContentType != null && !claimedContentType.isBlank()) {
            if (!fileType.getMimeType().equalsIgnoreCase(claimedContentType.trim())) {
                throw new BadRequestException(
                        "Claimed Content-Type '" + claimedContentType + "' does not match extension " + fileType.getExtension() +
                                ". Expected: " + fileType.getMimeType()
                );
            }
        }

        return fileType;
    }

    public void validateFileSignature(FileType expectedType, byte[] headerBytes) {
        if (headerBytes == null || headerBytes.length < 4) {
            throw new BadRequestException("Uploaded object is too short or empty to verify file signature.");
        }

        if (expectedType == FileType.PDF) {
            if (!startsWith(headerBytes, PDF_MAGIC)) {
                throw new BadRequestException("File signature validation failed. Uploaded file is not a valid PDF document.");
            }
        } else if (expectedType == FileType.DOCX || expectedType == FileType.PPTX) {
            if (!startsWith(headerBytes, ZIP_MAGIC)) {
                throw new BadRequestException("File signature validation failed. Uploaded file is not a valid OpenXML document (missing PK zip signature).");
            }
        }
    }

    public void validateOfficeDocumentStructure(FileType expectedType, byte[] contentBytes) {
        boolean hasContentTypes = false;
        boolean hasOfficeStructure = false;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(contentBytes))) {
            ZipEntry entry;
            int entriesCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entriesCount++;
                if (entriesCount > 500) {
                    break;
                }
                String name = entry.getName();
                if (name.contains("..")) {
                    throw new BadRequestException("Archive contains dangerous path traversal references.");
                }
                if (name.equals("[Content_Types].xml")) {
                    hasContentTypes = true;
                }
                if (expectedType == FileType.DOCX && (name.startsWith("word/") || name.equals("word/document.xml"))) {
                    hasOfficeStructure = true;
                }
                if (expectedType == FileType.PPTX && (name.startsWith("ppt/") || name.equals("ppt/presentation.xml"))) {
                    hasOfficeStructure = true;
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse OpenXML document structure: " + e.getMessage());
        }

        if (!hasContentTypes && !hasOfficeStructure) {
            throw new BadRequestException("File signature verification failed: not a valid " + expectedType.name() + " OpenXML document.");
        }
    }

    public String computeSha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            log.error("Failed to compute SHA-256 hash", e);
            throw new BadRequestException("Failed to calculate SHA-256 hash of uploaded file.");
        }
    }

    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

