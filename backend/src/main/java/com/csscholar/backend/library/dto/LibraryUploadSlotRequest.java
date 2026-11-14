package com.csscholar.backend.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LibraryUploadSlotRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type is required (e.g. PDF, DOCX, PPTX)")
    private String fileType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @NotBlank(message = "Content type is required")
    private String contentType;

    public LibraryUploadSlotRequest() {
    }

    public LibraryUploadSlotRequest(String fileName, String fileType, Long fileSize, String contentType) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

