package com.csscholar.backend.document.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StorageKeyGenerator {

    public String generateStorageKey(UUID workspaceId, UUID documentId, String originalFilename) {
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) {
            ext = originalFilename.substring(dotIdx).toLowerCase();
        }

        String baseName = dotIdx >= 0 ? originalFilename.substring(0, dotIdx) : originalFilename;
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        if (sanitized.isBlank()) {
            sanitized = "document";
        }

        return String.format("workspaces/%s/documents/%s/%s%s", workspaceId, documentId, sanitized, ext);
    }
}

