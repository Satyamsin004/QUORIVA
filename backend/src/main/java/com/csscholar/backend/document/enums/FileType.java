package com.csscholar.backend.document.enums;

import java.util.Arrays;
import java.util.Optional;

public enum FileType {
    PDF("application/pdf", ".pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx");

    private final String mimeType;
    private final String extension;

    FileType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public static Optional<FileType> fromMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.mimeType.equalsIgnoreCase(mimeType.trim()))
                .findFirst();
    }

    public static Optional<FileType> fromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return Optional.empty();
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.extension.equalsIgnoreCase(ext))
                .findFirst();
    }
}

